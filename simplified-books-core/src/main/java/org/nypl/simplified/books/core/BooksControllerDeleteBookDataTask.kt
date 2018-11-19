package org.nypl.simplified.books.core

import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

internal class BooksControllerDeleteBookDataTask(
  private val bookStatus: BooksStatusCacheType,
  private val bookDatabase: BookDatabaseType,
  private val bookID: BookID,
  private val needsAuthentication: Boolean) : Callable<Unit> {

  private val log = LoggerFactory.getLogger(BooksControllerDeleteBookDataTask::class.java)

  override fun call() {
    try {
      this.log.debug("[{}]: deleting book data", this.bookID.shortID)

      val databaseEntry = this.bookDatabase.databaseOpenExistingEntry(this.bookID)
      val snap = databaseEntry.entryDeleteBookData()
      val status = BookStatus.fromSnapshot(this.bookID, snap)
      this.bookStatus.booksStatusUpdate(status)

      // destroy entry, this is needed after deletion has been broadcasted, so the book doesn't stay in loans,
      // especially needed for collection without auth requirement where no syn is happening
      if (!this.needsAuthentication) {
        databaseEntry.entryDestroy()
      }
    } catch (e: Throwable) {
      this.log.error("[{}]: could not destroy book data: ", this.bookID.shortID, e)
      throw e
    } finally {
      this.log.debug("[{}]: deletion completed", this.bookID.shortID)
    }
  }
}
