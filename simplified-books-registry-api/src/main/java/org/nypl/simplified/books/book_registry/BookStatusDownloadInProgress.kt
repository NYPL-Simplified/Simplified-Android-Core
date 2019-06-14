package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.nypl.simplified.books.api.BookID

/**
 * The given book is currently downloading.
 */

class BookStatusDownloadInProgress(

  /**
   * The book ID
   */

  val id: BookID,

  /**
   * The current number of downloaded bytes
   */

  val currentTotalBytes: Long,

  /**
   * The expected total bytes
   */

  val expectedTotalBytes: Long,

  /**
   * The end date of the loan, if any
   */

  val loanEndDate: OptionType<DateTime>,

  val detailMessage: String) : BookStatusDownloadingType {

  override fun getID(): BookID {
    return this.id
  }

  override fun getLoanExpiryDate(): OptionType<DateTime> {
    return this.loanEndDate
  }

  override fun getPriority(): BookStatusPriorityOrdering {
    return BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_IN_PROGRESS
  }

  override fun <A, E : Exception> matchBookDownloadingStatus(
    m: BookStatusDownloadingMatcherType<A, E>): A {
    return m.onBookStatusDownloadInProgress(this)
  }

  override fun <A, E : Exception> matchBookLoanedStatus(
    m: BookStatusLoanedMatcherType<A, E>): A {
    return m.onBookStatusDownloading(this)
  }

  override fun <A, E : Exception> matchBookStatus(
    m: BookStatusMatcherType<A, E>): A {
    return m.onBookStatusLoanedType(this)
  }

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusDownloadInProgress ")
    b.append(this.id)
    b.append(" [")
    b.append(this.currentTotalBytes)
    b.append("/")
    b.append(this.expectedTotalBytes)
    b.append("]]")
    return b.toString()
  }
}
