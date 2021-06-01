package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.books.api.BookID
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.SortedMap
import java.util.concurrent.ConcurrentSkipListMap

class BookRegistry private constructor(
  private val books: ConcurrentSkipListMap<BookID, BookWithStatus>
) : BookRegistryType {

  private val logger =
    LoggerFactory.getLogger(BookRegistry::class.java)
  private val booksReadOnly: SortedMap<BookID, BookWithStatus> =
    Collections.unmodifiableSortedMap(this.books)
  private val observable: PublishSubject<BookStatusEvent> =
    PublishSubject.create()

  override fun books(): SortedMap<BookID, BookWithStatus> {
    return this.booksReadOnly
  }

  override fun bookEvents(): Observable<BookStatusEvent> {
    return this.observable
  }

  override fun bookStatus(id: BookID): OptionType<BookStatus> {
    return this.book(id).map(FunctionType<BookWithStatus, BookStatus>(BookWithStatus::status))
  }

  override fun book(id: BookID): OptionType<BookWithStatus> {
    return Option.of(this.books[id])
  }

  override fun update(status: BookWithStatus) {
    val oldStatus = this.books[status.book.id]
    this.books[status.book.id] = status
    this.publishUpdateEvent(oldStatus, status)
  }

  private fun publishUpdateEvent(oldStatus: BookWithStatus?, newStatus: BookWithStatus) {
    if (newStatus.status == oldStatus?.status) {
      return
    }

    val event =
      if (oldStatus == null) {
        BookStatusEvent.BookStatusEventAdded(
          bookId = newStatus.book.id,
          statusNow = newStatus.status
        )
      } else {
        BookStatusEvent.BookStatusEventChanged(
          bookId = oldStatus.book.id,
          statusPrevious = oldStatus.status,
          statusNow = newStatus.status
        )
      }

    this.observable.onNext(event)
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
    val entries = this.books.toMap()
    for (entry in entries) {
      this.observable.onNext(BookStatusEvent.BookStatusEventRemoved(entry.key, entry.value.status))
    }
    this.books.clear()
  }

  override fun clearFor(id: BookID) {
    val oldStatus = this.books.remove(id)
    if (oldStatus != null) {
      this.observable.onNext(BookStatusEvent.BookStatusEventRemoved(id, oldStatus.status))
    }
  }

  companion object {
    fun create(): BookRegistryType {
      return BookRegistry(ConcurrentSkipListMap())
    }
  }
}
