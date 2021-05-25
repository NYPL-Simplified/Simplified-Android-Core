package org.nypl.simplified.tests.books.reader.bookmarks

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import io.reactivex.Observable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceProviderType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType.SyncEnableResult
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks

class NullReaderBookmarkService(
  val events: Observable<ReaderBookmarkEvent>
) : ReaderBookmarkServiceType {

  override fun close() {
  }

  override val bookmarkEvents: Observable<ReaderBookmarkEvent>
    get() = this.events

  override fun bookmarkSyncEnable(accountID: AccountID, enabled: Boolean): FluentFuture<SyncEnableResult> {
    return FluentFuture.from(Futures.immediateFuture(SyncEnableResult.SYNC_ENABLE_NOT_SUPPORTED))
  }

  override fun bookmarkCreate(accountID: AccountID, bookmark: Bookmark): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

  override fun bookmarkDelete(accountID: AccountID, bookmark: Bookmark): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

  override fun bookmarkLoad(accountID: AccountID, book: BookID): FluentFuture<ReaderBookmarks> {
    return FluentFuture.from(
      Futures.immediateFuture(
        ReaderBookmarks(
          lastRead = null,
          bookmarks = listOf()
        )
      )
    )
  }

  companion object : ReaderBookmarkServiceProviderType {
    override fun createService(
      requirements: ReaderBookmarkServiceProviderType.Requirements
    ): ReaderBookmarkServiceType {
      return NullReaderBookmarkService(requirements.events)
    }
  }
}
