package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.files.FileUtilities
import java.io.File
import java.io.IOException

/**
 * Operations on PDF formats in database entries.
 */

internal class DatabaseFormatHandlePDF internal constructor(
  private val parameters: DatabaseFormatHandleParameters)
  : BookDatabaseEntryFormatHandlePDF() {

  private val fileBook: File =
    File(this.parameters.directory, "pdf-book.pdf")

  private val formatLock: Any = Any()
  private var formatRef: BookFormat.BookFormatPDF =
    synchronized(this.formatLock) {
      loadInitial(fileBook = this.fileBook)
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

  companion object {

    @Throws(IOException::class)
    private fun loadInitial(fileBook: File): BookFormat.BookFormatPDF {
      return BookFormat.BookFormatPDF(file = if (fileBook.isFile) fileBook else null)
    }
  }
}