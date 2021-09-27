package org.nypl.simplified.books.book_database

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import net.jcip.annotations.GuardedBy
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkJSON
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkExplicit
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkLastReadLocation
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException

/**
 * Operations on EPUB formats in database entries.
 */

internal class DatabaseFormatHandleEPUB internal constructor(
  private val parameters: DatabaseFormatHandleParameters
) : BookDatabaseEntryFormatHandleEPUB() {

  private val fileBook: File =
    File(this.parameters.directory, "epub-book.epub")
  private val fileLastRead: File =
    File(this.parameters.directory, "epub-meta_last_read.json")
  private val fileLastReadTmp: File =
    File(this.parameters.directory, "epub-meta_last_read.json.tmp")
  private val fileBookmarks: File =
    File(this.parameters.directory, "epub-meta_bookmarks.json")
  private val fileBookmarksTmp: File =
    File(this.parameters.directory, "epub-meta_bookmarks.json.tmp")

  private val dataLock: Any = Any()

  @GuardedBy("dataLock")
  private var drmHandleRef: BookDRMInformationHandle

  init {
    val drmHandleInitial =
      BookDRMInformationHandles.open(
        directory = this.parameters.directory,
        format = this.formatDefinition,
        bookFormats = this.parameters.bookFormatSupport,
        onUpdate = this::onDRMUpdated
      )

    if (drmHandleInitial == null) {
      try {
        if (this.fileBook.isDirectory) {
          DirectoryUtilities.directoryDelete(this.fileBook)
        } else {
          FileUtilities.fileDelete(this.fileBook)
        }
      } catch (e: Exception) {
        // Not much we can do about this.
      }

      val drmHandleNext =
        BookDRMInformationHandles.open(
          directory = this.parameters.directory,
          format = this.formatDefinition,
          bookFormats = this.parameters.bookFormatSupport,
          onUpdate = this::onDRMUpdated
        ) ?: throw IllegalStateException("Still could not open a DRM handle!")

      this.drmHandleRef = drmHandleNext
    } else {
      this.drmHandleRef = drmHandleInitial
    }
  }

  @GuardedBy("dataLock")
  private var formatRef: BookFormat.BookFormatEPUB =
    synchronized(this.dataLock) {
      loadInitial(
        objectMapper = this.parameters.objectMapper,
        fileBookmarks = this.fileBookmarks,
        fileBook = this.fileBook,
        fileLastRead = this.fileLastRead,
        contentType = this.parameters.contentType,
        drmInfo = this.drmInformationHandle.info
      )
    }

  private fun onDRMUpdated() {
    this.parameters.onUpdated.invoke(this.refreshDRM())
  }

  private fun refreshDRM(): BookFormat.BookFormatEPUB {
    return synchronized(this.dataLock) {
      this.formatRef = this.formatRef.copy(drmInformation = this.drmInformationHandle.info)
      this.formatRef
    }
  }

  override val format: BookFormat.BookFormatEPUB
    get() = synchronized(this.dataLock, this::formatRef)

  override val drmInformationHandle: BookDRMInformationHandle
    get() = synchronized(this.dataLock, this::drmHandleRef)

  override fun setDRMKind(kind: BookDRMKind) {
    synchronized(this.dataLock) {
      val oldRef = (this.drmHandleRef as BookDRMInformationHandleBase)
      this.drmHandleRef = BookDRMInformationHandles.create(
        directory = this.parameters.directory,
        format = this.formatDefinition,
        drmKind = kind,
        onUpdate = this::onDRMUpdated
      )
      oldRef.close()
      this.onDRMUpdated()
    }
  }

  override fun deleteBookData() {
    val newFormat = synchronized(this.dataLock) {
      if (this.fileBook.isDirectory) {
        DirectoryUtilities.directoryDelete(this.fileBook)
      } else {
        FileUtilities.fileDelete(this.fileBook)
      }
      this.formatRef = this.formatRef.copy(file = null)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun copyInBook(file: File) {
    val newFormat = synchronized(this.dataLock) {
      if (file.isDirectory) {
        DirectoryUtilities.directoryCopy(file, this.fileBook)
      } else {
        FileUtilities.fileCopy(file, this.fileBook)
      }

      this.formatRef = this.formatRef.copy(file = this.fileBook)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setLastReadLocation(bookmark: Bookmark?) {
    val newFormat = synchronized(this.dataLock) {
      if (bookmark != null) {
        Preconditions.checkArgument(
          bookmark.kind == ReaderBookmarkLastReadLocation,
          "Must use a last-read-location bookmark"
        )

        FileUtilities.fileWriteUTF8Atomically(
          this.fileLastRead,
          this.fileLastReadTmp,
          BookmarkJSON.serializeToString(this.parameters.objectMapper, bookmark)
        )
      } else {
        FileUtilities.fileDelete(this.fileLastRead)
      }

      this.formatRef = this.formatRef.copy(lastReadLocation = bookmark)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setBookmarks(bookmarks: List<Bookmark>) {
    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileWriteUTF8Atomically(
        this.fileBookmarks,
        this.fileBookmarksTmp,
        BookmarkJSON.serializeToString(this.parameters.objectMapper, bookmarks)
      )
      this.formatRef = this.formatRef.copy(bookmarks = bookmarks)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  companion object {

    @Throws(IOException::class)
    private fun loadInitial(
      objectMapper: ObjectMapper,
      fileBook: File,
      fileBookmarks: File,
      fileLastRead: File,
      contentType: MIMEType,
      drmInfo: BookDRMInformation
    ): BookFormat.BookFormatEPUB {
      return BookFormat.BookFormatEPUB(
        bookmarks = loadBookmarksIfPresent(objectMapper, fileBookmarks),
        file = if (fileBook.exists()) fileBook else null,
        lastReadLocation = loadLastReadLocationIfPresent(objectMapper, fileLastRead),
        contentType = contentType,
        drmInformation = drmInfo
      )
    }

    @Throws(IOException::class)
    private fun loadBookmarksIfPresent(
      objectMapper: ObjectMapper,
      fileBookmarks: File
    ): List<Bookmark> {
      return if (fileBookmarks.isFile) {
        loadBookmarks(
          objectMapper = objectMapper,
          fileBookmarks = fileBookmarks
        )
      } else {
        listOf()
      }
    }

    private fun loadBookmarks(
      objectMapper: ObjectMapper,
      fileBookmarks: File
    ): List<Bookmark> {
      val tree = objectMapper.readTree(fileBookmarks)
      val array = JSONParserUtilities.checkArray(null, tree)
      return array.map { node ->
        BookmarkJSON.deserializeFromJSON(
          objectMapper = objectMapper,
          kind = ReaderBookmarkExplicit,
          node = node
        )
      }
    }

    @Throws(IOException::class)
    private fun loadLastReadLocationIfPresent(
      objectMapper: ObjectMapper,
      fileLastRead: File
    ): Bookmark? {
      return if (fileLastRead.isFile) {
        loadLastReadLocation(
          objectMapper = objectMapper,
          fileLastRead = fileLastRead
        )
      } else {
        null
      }
    }

    @Throws(IOException::class)
    private fun loadLastReadLocation(
      objectMapper: ObjectMapper,
      fileLastRead: File
    ): Bookmark {
      val serialized = FileUtilities.fileReadUTF8(fileLastRead)
      return BookmarkJSON.deserializeFromString(
        objectMapper = objectMapper,
        kind = ReaderBookmarkLastReadLocation,
        serialized = serialized
      )
    }
  }
}
