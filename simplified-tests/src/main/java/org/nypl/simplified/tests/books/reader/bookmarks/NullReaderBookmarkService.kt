package org.nypl.simplified.tests.books.reader.bookmarks

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceProviderType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarks
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableType

class NullReaderBookmarkService(
  val events: ObservableType<ReaderBookmarkEvent>) : ReaderBookmarkServiceType {

  override fun close() {

  }

  override val bookmarkEvents: ObservableReadableType<ReaderBookmarkEvent>
    get() = this.events

  override fun bookmarkCreate(accountID: AccountID, bookmark: ReaderBookmark): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

  override fun bookmarkDelete(accountID: AccountID, bookmark: ReaderBookmark): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

  override fun bookmarkLoad(accountID: AccountID, book: BookID): FluentFuture<ReaderBookmarks> {
    return FluentFuture.from(Futures.immediateFuture(
      ReaderBookmarks(
        lastRead = null,
        bookmarks = listOf())))
  }

  companion object : ReaderBookmarkServiceProviderType {
    override fun createService(
      requirements: ReaderBookmarkServiceProviderType.Requirements): ReaderBookmarkServiceType {
      return NullReaderBookmarkService(requirements.events)
    }
  }
}