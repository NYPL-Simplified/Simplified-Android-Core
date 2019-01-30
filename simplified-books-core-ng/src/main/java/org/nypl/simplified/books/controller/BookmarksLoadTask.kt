package org.nypl.simplified.books.controller

import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.controller.BookmarksControllerType.Bookmarks
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class BookmarksLoadTask(
  val account: AccountType,
  val bookID: BookID): Callable<Bookmarks> {

  private val logger = LoggerFactory.getLogger(BookmarksLoadTask::class.java)

  override fun call(): Bookmarks {
    try {
      val entry =
        this.account.bookDatabase().entry(this.bookID)
      val format =
        entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      return if (format != null) {
        Bookmarks(
          lastRead = format.format.lastReadLocation,
          bookmarks = format.format.bookmarks)
      } else {
        this.logger.error("[{}]: wrong format for bookmarks", this.bookID.brief())
        Bookmarks(lastRead = null, bookmarks = listOf())
      }
    } catch (e: Exception) {
      this.logger.error("[{}]: failed to load bookmarks: ", this.bookID.brief(), e)
      throw e
    }
  }
}