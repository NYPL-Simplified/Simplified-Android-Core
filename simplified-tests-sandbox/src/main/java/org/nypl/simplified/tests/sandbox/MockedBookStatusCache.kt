package org.nypl.simplified.tests.sandbox

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BookStatusType
import org.nypl.simplified.books.core.BooksStatusCacheType
import org.nypl.simplified.books.core.FeedEntryType
import java.util.Observer
import java.util.concurrent.ConcurrentHashMap

class MockedBookStatusCache : BooksStatusCacheType {

  val statusMap: ConcurrentHashMap<BookID, BookStatusType> = ConcurrentHashMap()

  override fun booksStatusUpdateIfMoreImportant(s: BookStatusType?) {
    throw UnimplementedCodeException()
  }

  override fun booksObservableDeleteObserver(o: Observer?) {
    throw UnimplementedCodeException()
  }

  override fun booksStatusGet(id: BookID?): OptionType<BookStatusType> {
    return Option.of(this.statusMap[id])
  }

  override fun booksRevocationFeedEntryGet(id: BookID?): OptionType<FeedEntryType> {
    throw UnimplementedCodeException()
  }

  override fun booksRevocationFeedEntryUpdate(e: FeedEntryType?) {
    throw UnimplementedCodeException()
  }

  override fun booksObservableAddObserver(o: Observer?) {

  }

  override fun booksObservableDeleteAllObservers() {
    throw UnimplementedCodeException()
  }

  override fun booksObservableNotify(id: BookID?) {
    throw UnimplementedCodeException()
  }

  override fun booksStatusClearAll() {
    throw UnimplementedCodeException()
  }

  override fun booksStatusUpdate(s: BookStatusType?) {
    throw UnimplementedCodeException()
  }

  override fun booksStatusClearFor(book_id: BookID?) {
    throw UnimplementedCodeException()
  }

}