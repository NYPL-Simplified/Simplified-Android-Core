package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.books.api.BookID

/**
 * The given book is currently placed on hold.
 */

data class BookStatusHeld(

  /**
   * The book ID
   */

  val id: BookID,

  /**
   * The current position of the user in the queue
   */

  val queuePosition: OptionType<Int>,

  /**
   * @return The current position of the user in the queue
   */

  val startDate: OptionType<DateTime>,

  /**
   * @return The approximate date that the book will become available
   */

  val endDate: OptionType<DateTime>,

  /**
   * @return `true` iff the hold is revocable
   */

  val isRevocable: Boolean) : BookStatusType {


  override fun getID(): BookID = this.id

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_HELD

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A =
    m.onBookStatusHeld(this)

  override fun toString(): String {
    val fmt = ISODateTimeFormat.dateTime()
    val b = StringBuilder(128)
    b.append("[BookStatusHeld ")
    b.append(this.id)
    b.append(" ")
    b.append(this.queuePosition)
    b.append(" ")
    b.append(this.startDate.map<String> { fmt.print(it) })
    b.append(" ")
    b.append(this.endDate.map<String> { fmt.print(it) })
    b.append(" revocable=")
    b.append(this.isRevocable)
    b.append("]")
    return b.toString()
  }
}
