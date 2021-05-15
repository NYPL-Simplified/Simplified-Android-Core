package org.nypl.simplified.ui.catalog

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult

interface CatalogPagedViewListener {

  fun registerObserver(feedEntry: FeedEntry.FeedEntryOPDS, callback: (BookWithStatus) -> Unit)

  fun unregisterObserver(feedEntry: FeedEntry.FeedEntryOPDS, callback: (BookWithStatus) -> Unit)

  fun openViewer(book: Book, format: BookFormat)

  fun openBookDetail(opdsEntry: FeedEntry.FeedEntryOPDS)

  fun showTaskError(book: Book, result: TaskResult.Failure<*>)

  fun dismissBorrowError(feedEntry: FeedEntry.FeedEntryOPDS)

  fun dismissRevokeError(feedEntry: FeedEntry.FeedEntryOPDS)

  fun delete(feedEntry: FeedEntry.FeedEntryOPDS)

  fun borrowMaybeAuthenticated(book: Book)

  fun reserveMaybeAuthenticated(book: Book)

  fun revokeMaybeAuthenticated(book: Book)
}
