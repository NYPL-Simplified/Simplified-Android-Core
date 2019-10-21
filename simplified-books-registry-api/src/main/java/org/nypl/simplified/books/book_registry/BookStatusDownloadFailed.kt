package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import org.joda.time.DateTime
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables
import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * The given book failed to download properly.
 */

data class BookStatusDownloadFailed(

  /**
   *  The book ID
   */

  val id: BookID,

  /**
   * The list of steps that lead to the failure.
   */

  val result: TaskResult.Failure<BookStatusDownloadErrorDetails, Unit>,

  /**
   * The expiry date of the loan, if any
   */

  val loanEndDate: OptionType<DateTime>) : BookStatusDownloadingType, PresentableErrorType {

  override val attributes: Map<String, String>
    get() = Presentables.collectAttributes(this.result.errors())

  override val message: String
    get() = this.result.steps.last().resolution.message

  override fun getID(): BookID =
    this.id

  override fun getLoanExpiryDate(): OptionType<DateTime> =
    this.loanEndDate

  override fun <A, E : Exception> matchBookDownloadingStatus(
    m: BookStatusDownloadingMatcherType<A, E>): A = m.onBookStatusDownloadFailed(this)

  override fun <A, E : Exception> matchBookLoanedStatus(
    m: BookStatusLoanedMatcherType<A, E>): A = m.onBookStatusDownloading(this)

  override fun <A, E : Exception> matchBookStatus(
    m: BookStatusMatcherType<A, E>): A = m.onBookStatusLoanedType(this)

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusDownloadFailed ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_FAILED
}
