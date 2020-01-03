package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.None
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.OptionVisitorType
import com.io7m.jfunctional.Some
import io.reactivex.Observable

import org.nypl.simplified.books.api.BookID

import java.util.NoSuchElementException
import java.util.SortedMap

/**
 * The type of readable book registries.
 */

interface BookRegistryReadableType {

  /**
   * @return A read-only map of the known books
   */

  fun books(): SortedMap<BookID, BookWithStatus>

  /**
   * @return An observable that publishes book status events
   */

  fun bookEvents(): Observable<BookStatusEvent>

  /**
   * @param id The book ID
   * @return The status for the given book, if any.
   */

  fun bookStatus(id: BookID): OptionType<BookStatus>

  /**
   * @param id The book ID
   * @return The status for the given book, if any.
   */

  fun bookStatusOrNull(id: BookID): BookStatus? {
    val statusOpt = this.bookStatus(id)
    return if (statusOpt is Some<BookStatus>) {
      statusOpt.get()
    } else {
      null
    }
  }

  /**
   * @param id The book ID
   * @return The registered book, if any
   */

  fun book(id: BookID): OptionType<BookWithStatus>

  /**
   * @param id The book ID
   * @return The registered book, if any
   */

  fun bookOrNull(id: BookID): BookWithStatus? {
    val statusOpt = this.book(id)
    return if (statusOpt is Some<BookWithStatus>) {
      statusOpt.get()
    } else {
      null
    }
  }

  /**
   * @param id The book ID
   * @return The registered book
   * @throws NoSuchElementException If the given book does not exist
   */

  @Throws(NoSuchElementException::class)
  fun bookOrException(id: BookID): BookWithStatus {
    return book(id).accept(object : OptionVisitorType<BookWithStatus, BookWithStatus> {
      override fun none(none: None<BookWithStatus>): BookWithStatus {
        throw NoSuchElementException("No such book: " + id.value())
      }

      override fun some(some: Some<BookWithStatus>): BookWithStatus {
        return some.get()
      }
    })
  }
}
