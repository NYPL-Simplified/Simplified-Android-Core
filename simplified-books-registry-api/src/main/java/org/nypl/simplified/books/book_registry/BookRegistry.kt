package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_CHANGED
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_REMOVED
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableType
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.HashSet
import java.util.SortedMap
import java.util.concurrent.ConcurrentSkipListMap

class BookRegistry private constructor(
  private val books: ConcurrentSkipListMap<BookID, BookWithStatus>
) : BookRegistryType {

  private val logger =
    LoggerFactory.getLogger(BookRegistry::class.java)
  private val booksReadOnly: SortedMap<BookID, BookWithStatus> =
    Collections.unmodifiableSortedMap(this.books)
  private val observable: ObservableType<BookStatusEvent> =
    Observable.create()

  override fun books(): SortedMap<BookID, BookWithStatus> {
    return this.booksReadOnly
  }

  override fun bookEvents(): ObservableReadableType<BookStatusEvent> {
    return this.observable
  }

  override fun bookStatus(id: BookID): OptionType<BookStatus> {
    return this.book(id).map(FunctionType<BookWithStatus, BookStatus>(BookWithStatus::status))
  }

  override fun book(id: BookID): OptionType<BookWithStatus> {
    return Option.of(this.books[id])
  }

  override fun update(status: BookWithStatus) {
    this.books[status.book.id] = status
    this.observable.send(BookStatusEvent.create(status.book.id, BOOK_CHANGED))
  }

  override fun updateIfStatusIsMoreImportant(status: BookWithStatus) {
    val current = this.books[status.book.id]
    if (current != null) {
      val currentPri = current.status.priority
      val updatePri = status.status.priority

      if (currentPri.priority <= updatePri.priority) {
        this.logger.debug("current {} <= {}, updating", current, status)
        this.update(status)
        return
      }

      this.logger.debug("current {} > {}, not updating", current, status)
    } else {
      this.update(status)
    }
  }

  override fun clear() {
    val ids = HashSet(this.books.keys)
    this.books.clear()
    for (id in ids) {
      this.observable.send(BookStatusEvent.create(id, BOOK_REMOVED))
    }
  }

  override fun clearFor(id: BookID) {
    this.books.remove(id)
    this.observable.send(BookStatusEvent.create(id, BOOK_REMOVED))
  }

  companion object {
    fun create(): BookRegistryType {
      return BookRegistry(ConcurrentSkipListMap())
    }
  }
}
