package org.nypl.simplified.books.controller

import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.BookID
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class BookmarksUpdateTask(
  val account: AccountType,
  val bookID: BookID,
  val bookmarks: (BookmarksControllerType.Bookmarks) -> BookmarksControllerType.Bookmarks)
  : Callable<BookmarksControllerType.Bookmarks> {

  private val logger = LoggerFactory.getLogger(BookmarksUpdateTask::class.java)

  override fun call(): BookmarksControllerType.Bookmarks {
    try {
      val entry =
        this.account.bookDatabase().entry(this.bookID)
      val format =
        entry.findFormatHandle(BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB::class.java)

      return if (format != null) {
        val current =
          BookmarksControllerType.Bookmarks(
            lastRead = format.format.lastReadLocation,
            bookmarks = format.format.bookmarks)
        val next =
          this.bookmarks.invoke(current)

        format.setLastReadLocation(next.lastRead)
        format.setBookmarks(next.bookmarks)
        next
      } else {
        this.logger.error("[{}]: wrong format for bookmarks", this.bookID.brief())
        BookmarksControllerType.Bookmarks(lastRead = null, bookmarks = listOf())
      }
    } catch (e: Exception) {
      this.logger.error("[{}]: failed to load bookmarks: ", this.bookID.brief(), e)
      throw e
    }
  }
}