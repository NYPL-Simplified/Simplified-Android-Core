package org.nypl.simplified.books.core

import com.io7m.junreachable.UnimplementedCodeException
import org.slf4j.LoggerFactory

internal class BooksControllerDeleteBookDataTask(
  private val bookStatus: BooksStatusCacheType,
  private val bookDatabase: BookDatabaseType,
  private val bookID: BookID,
  private val needsAuthentication: Boolean) : Runnable {

  override fun run() {
    try {
      val databaseEntry =
        this.bookDatabase.databaseOpenExistingEntry(this.bookID)

      for (format in databaseEntry.entryFormatHandles()) {
        when (format) {
          is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB ->
            format.deleteBookData()
          is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook ->
            throw UnimplementedCodeException()
        }
      }

      val snap = databaseEntry.entryGetSnapshot()
      val status = BookStatus.fromSnapshot(this.bookID, snap)
      this.bookStatus.booksStatusUpdate(status)

      // destroy entry, this is needed after deletion has been broadcasted, so the book doesn't stay in loans,
      // especially needed for collection without auth requirement where no syn is happening
      if (!this.needsAuthentication) {
        databaseEntry.entryDestroy()
      }
    } catch (e: Throwable) {
      LOG.error("[{}]: could not destroy book data: ", this.bookID.shortID, e)
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(BooksControllerDeleteBookDataTask::class.java)
  }
}
