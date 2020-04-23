package org.nypl.simplified.books.book_database

import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.files.FileUtilities
import java.io.File
import java.io.IOException

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

  private val formatLock: Any = Any()
  private var formatRef: BookFormat.BookFormatPDF =
    synchronized(this.formatLock) {
      loadInitial(
        fileBook = this.fileBook,
        fileLastRead = this.fileLastRead,
        contentType = this.parameters.contentType
      )
    }

  override val format: BookFormat.BookFormatPDF
    get() = synchronized(this.formatLock) { this.formatRef }

  override val formatDefinition: BookFormats.BookFormatDefinition
    get() = BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF

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

  override fun setLastReadLocation(pageNumber: Int?) {
    val newFormat = synchronized(this.formatLock) {
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
      contentType: MIMEType
    ): BookFormat.BookFormatPDF {
      return BookFormat.BookFormatPDF(
        file = if (fileBook.isFile) fileBook else null,
        lastReadLocation = loadLastReadLocationIfPresent(fileLastRead),
        contentType = contentType
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
