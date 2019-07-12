package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * The given book could not be revoked (hold cancelled, loan returned, etc).
 */

data class BookStatusRevokeFailed(

  /**
   * The book ID
   */

  val id: BookID,

  /**
   * The list of steps that lead to the failure.
   */

  val result: TaskResult.Failure<BookStatusRevokeErrorDetails, Unit>) : BookStatusType {

  override fun getID(): BookID =
    this.id

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A =
    m.onBookStatusRevokeFailed(this)

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusRevokeFailed ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_FAILED
}
