package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.nypl.simplified.books.api.BookID

/**
 * The given book is downloaded and available for reading.
 */

data class BookStatusDownloaded(

  /**
   * The book ID
   */

  val id: BookID,

  /**
   * The expiry date of the loan, if any
   */

  val loanEndDate: OptionType<DateTime>,

  /**
   * @return `true` iff the book is returnable
   */

  val isReturnable: Boolean) : BookStatusDownloadedType {

  override fun getID(): BookID =
    this.id

  override fun getLoanExpiryDate(): OptionType<DateTime> =
    this.loanEndDate

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOADED

  override fun <A, E : Exception> matchBookLoanedStatus(m: BookStatusLoanedMatcherType<A, E>): A =
    m.onBookStatusDownloaded(this)

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A =
    m.onBookStatusLoanedType(this)

  override fun toString(): String {
    val b = StringBuilder(64)
    b.append("[BookStatusDownloaded ")
    b.append(this.id)
    b.append(" returnable=")
    b.append(this.isReturnable)
    b.append("]")
    return b.toString()
  }
}
