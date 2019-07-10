package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.nypl.simplified.books.api.BookID

/**
 * The given book is being requested for download, but the download has not
 * actually started yet.
 */

data class BookStatusRequestingDownload(

  /**
   * The book ID
   */

  val id: BookID,

  /**
   * The end date of the loan, if any
   */

  val loanEndDate: OptionType<DateTime>,

  val detailMessage: String) : BookStatusLoanedType {

  override fun getID(): BookID =
    this.id

  override fun <A, E : Exception> matchBookLoanedStatus(m: BookStatusLoanedMatcherType<A, E>): A =
    m.onBookStatusRequestingDownload(this)

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A =
    m.onBookStatusLoanedType(this)

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusRequestingDownload ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }

  override fun getLoanExpiryDate(): OptionType<DateTime> =
    this.loanEndDate

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_REQUESTING
}
