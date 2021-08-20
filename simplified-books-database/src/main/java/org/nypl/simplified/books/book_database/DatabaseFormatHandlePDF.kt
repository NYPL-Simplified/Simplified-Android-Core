package org.nypl.simplified.books.book_database

import net.jcip.annotations.GuardedBy
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.files.FileUtilities
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException

/**
 * Operations on PDF formats in database entries.
 */

internal class DatabaseFormatHandlePDF internal constructor(
  private val parameters: DatabaseFormatHandleParameters
) : BookDatabaseEntryFormatHandlePDF() {

  private val fileBook: File =
    File(this.parameters.directory, "pdf-book.pdf")
  private val fileLastRead: File =
    File(this.parameters.directory, "pdf-meta_last_read.json")
  private val fileLastReadTmp: File =
    File(this.parameters.directory, "pdf-meta_last_read.json.tmp")

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
        FileUtilities.fileDelete(this.fileBook)
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
  private var formatRef: BookFormat.BookFormatPDF =
    synchronized(this.dataLock) {
      loadInitial(
        fileBook = this.fileBook,
        fileLastRead = this.fileLastRead,
        contentType = this.parameters.contentType,
        drmInfo = this.drmInformationHandle.info
      )
    }

  private fun onDRMUpdated() {
    this.parameters.onUpdated.invoke(this.refreshDRM())
  }

  private fun refreshDRM(): BookFormat.BookFormatPDF {
    return synchronized(this.dataLock) {
      this.formatRef = this.formatRef.copy(drmInformation = this.drmInformationHandle.info)
      this.formatRef
    }
  }

  override val format: BookFormat.BookFormatPDF
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
      FileUtilities.fileDelete(this.fileBook)
      this.formatRef = this.formatRef.copy(file = null)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun copyInBook(file: File) {
    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileCopy(file, this.fileBook)
      this.formatRef = this.formatRef.copy(file = this.fileBook)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun setLastReadLocation(pageNumber: Int?) {
    val newFormat = synchronized(this.dataLock) {
      if (pageNumber != null) {
        FileUtilities.fileWriteUTF8Atomically(
          this.fileLastRead,
          this.fileLastReadTmp,
          pageNumber.toString()
        )
      } else {
        FileUtilities.fileDelete(this.fileLastRead)
      }

      this.formatRef = this.formatRef.copy(lastReadLocation = pageNumber)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  companion object {

    @Throws(IOException::class)
    private fun loadInitial(
      fileBook: File,
      fileLastRead: File,
      contentType: MIMEType,
      drmInfo: BookDRMInformation
    ): BookFormat.BookFormatPDF {
      return BookFormat.BookFormatPDF(
        file = if (fileBook.isFile) fileBook else null,
        lastReadLocation = loadLastReadLocationIfPresent(fileLastRead),
        contentType = contentType,
        drmInformation = drmInfo
      )
    }

    @Throws(IOException::class)
    private fun loadLastReadLocation(fileLastRead: File): Int {
      val serialized = FileUtilities.fileReadUTF8(fileLastRead)
      return serialized.toInt()
    }

    @Throws(IOException::class)
    private fun loadLastReadLocationIfPresent(
      fileLastRead: File
    ): Int? {
      return if (fileLastRead.isFile) {
        loadLastReadLocation(fileLastRead)
      } else {
        null
      }
    }
  }
}
