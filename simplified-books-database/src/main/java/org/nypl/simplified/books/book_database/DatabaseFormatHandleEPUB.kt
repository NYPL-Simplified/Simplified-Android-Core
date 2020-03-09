package org.nypl.simplified.books.book_database

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkJSON
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkExplicit
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkLastReadLocation
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Operations on EPUB formats in database entries.
 */

internal class DatabaseFormatHandleEPUB internal constructor(
  private val parameters: DatabaseFormatHandleParameters
) : BookDatabaseEntryFormatHandleEPUB() {

  private val fileAdobeRightsTmp: File =
    File(this.parameters.directory, "epub-rights_adobe.xml.tmp")
  private val fileAdobeRights: File =
    File(this.parameters.directory, "epub-rights_adobe.xml")
  private val fileAdobeMeta: File =
    File(this.parameters.directory, "epub-meta_adobe.json")
  private val fileAdobeMetaTmp: File =
    File(this.parameters.directory, "epub-meta_adobe.json.tmp")
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

  private val formatLock: Any = Any()
  private var formatRef: org.nypl.simplified.books.api.BookFormat.BookFormatEPUB =
    synchronized(this.formatLock) {
      loadInitial(
        fileAdobeRights = this.fileAdobeRights,
        fileAdobeMeta = this.fileAdobeMeta,
        fileBookmarks = this.fileBookmarks,
        fileBook = this.fileBook,
        fileLastRead = this.fileLastRead)
    }

  override val format: org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
    get() = synchronized(this.formatLock) { this.formatRef }

  override val formatDefinition: BookFormats.BookFormatDefinition
    get() = BOOK_FORMAT_EPUB

  override fun deleteBookData() {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileDelete(this.fileBook)
      this.formatRef = this.formatRef.copy(file = null)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun copyInBook(file: File) {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileCopy(file, this.fileBook)
      this.formatRef = this.formatRef.copy(file = this.fileBook)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setAdobeRightsInformation(loan: AdobeAdeptLoan?) {
    val newFormat = synchronized(this.formatLock) {
      if (loan != null) {
        FileUtilities.fileWriteBytesAtomically(
          this.fileAdobeRights,
          this.fileAdobeRightsTmp,
          loan.serialized.array())

        val jom = ObjectMapper()
        val o = jom.createObjectNode()
        o.put("loan-id", loan.id.value)
        o.put("returnable", loan.isReturnable)

        ByteArrayOutputStream().use { stream ->
          JSONSerializerUtilities.serialize(o, stream)

          FileUtilities.fileWriteUTF8Atomically(
            this.fileAdobeMeta,
            this.fileAdobeMetaTmp,
            stream.toString("UTF-8"))
        }

        this.formatRef =
          this.formatRef.copy(
            adobeRightsFile = this.fileAdobeRights,
            adobeRights = loan)

        this.formatRef
      } else {
        FileUtilities.fileDelete(this.fileAdobeMeta)
        FileUtilities.fileDelete(this.fileAdobeMetaTmp)
        FileUtilities.fileDelete(this.fileAdobeRights)
        FileUtilities.fileDelete(this.fileAdobeRightsTmp)

        this.formatRef =
          this.formatRef.copy(
            adobeRightsFile = null,
            adobeRights = null)

        this.formatRef
      }
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setLastReadLocation(bookmark: Bookmark?) {
    val newFormat = synchronized(this.formatLock) {
      if (bookmark != null) {
        Preconditions.checkArgument(
          bookmark.kind == ReaderBookmarkLastReadLocation,
          "Must use a last-read-location bookmark")

        FileUtilities.fileWriteUTF8Atomically(
          this.fileLastRead,
          this.fileLastReadTmp,
          BookmarkJSON.serializeToString(objectMapper, bookmark))
      } else {
        FileUtilities.fileDelete(this.fileLastRead)
      }

      this.formatRef = this.formatRef.copy(lastReadLocation = bookmark)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setBookmarks(bookmarks: List<Bookmark>) {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileWriteUTF8Atomically(
        this.fileBookmarks,
        this.fileBookmarksTmp,
        BookmarkJSON.serializeToString(objectMapper, bookmarks))
      this.formatRef = this.formatRef.copy(bookmarks = bookmarks)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  companion object {

    private val objectMapper = ObjectMapper()

    @Throws(IOException::class)
    private fun loadInitial(
      fileAdobeRights: File,
      fileAdobeMeta: File,
      fileBook: File,
      fileBookmarks: File,
      fileLastRead: File
    ): org.nypl.simplified.books.api.BookFormat.BookFormatEPUB {
      return org.nypl.simplified.books.api.BookFormat.BookFormatEPUB(
        adobeRightsFile = if (fileAdobeRights.isFile) fileAdobeRights else null,
        adobeRights = loadAdobeRightsInformationIfPresent(fileAdobeRights, fileAdobeMeta),
        bookmarks = loadBookmarksIfPresent(fileBookmarks),
        file = if (fileBook.isFile) fileBook else null,
        lastReadLocation = loadLastReadLocationIfPresent(fileLastRead))
    }

    @Throws(IOException::class)
    private fun loadBookmarksIfPresent(fileBookmarks: File): List<Bookmark> {
      return if (fileBookmarks.isFile) {
        loadBookmarks(fileBookmarks)
      } else {
        listOf()
      }
    }

    private fun loadBookmarks(fileBookmarks: File): List<Bookmark> {
      val tree = objectMapper.readTree(fileBookmarks)
      val array = JSONParserUtilities.checkArray(null, tree)
      return array.map { node ->
        BookmarkJSON.deserializeFromJSON(
          objectMapper = objectMapper,
          kind = ReaderBookmarkExplicit,
          node = node)
      }
    }

    @Throws(IOException::class)
    private fun loadLastReadLocationIfPresent(
      fileLastRead: File
    ): Bookmark? {
      return if (fileLastRead.isFile) {
        loadLastReadLocation(fileLastRead)
      } else {
        null
      }
    }

    @Throws(IOException::class)
    private fun loadLastReadLocation(fileLastRead: File): Bookmark {
      val serialized = FileUtilities.fileReadUTF8(fileLastRead)
      return BookmarkJSON.deserializeFromString(
        objectMapper, ReaderBookmarkLastReadLocation, serialized)
    }

    @Throws(IOException::class)
    private fun loadAdobeRightsInformationIfPresent(
      fileAdobeRights: File,
      fileAdobeMeta: File
    ): AdobeAdeptLoan? {
      return if (fileAdobeRights.isFile) {
        loadAdobeRightsInformation(fileAdobeRights, fileAdobeMeta)
      } else {
        null
      }
    }

    @Throws(IOException::class)
    private fun loadAdobeRightsInformation(
      fileAdobeRights: File,
      fileAdobeMeta: File
    ): AdobeAdeptLoan? {
      val serialized = FileUtilities.fileReadBytes(fileAdobeRights)
      val jn = objectMapper.readTree(fileAdobeMeta)
      val o = JSONParserUtilities.checkObject(null, jn)
      val loanID = AdobeLoanID(JSONParserUtilities.getString(o, "loan-id"))
      val returnable = JSONParserUtilities.getBoolean(o, "returnable")
      return AdobeAdeptLoan(loanID, ByteBuffer.wrap(serialized), returnable)
    }
  }
}
