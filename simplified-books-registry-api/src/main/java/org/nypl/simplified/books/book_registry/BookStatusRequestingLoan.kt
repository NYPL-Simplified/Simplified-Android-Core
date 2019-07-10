package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookID

/**
 * The given book is being requested but it is not yet known if the book is
 * loaned or not.
 */

data class BookStatusRequestingLoan(
  val id: BookID,
  val detailMessage: String) : BookStatusType {

  override fun getID(): BookID =
    this.id

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A {
    return m.onBookStatusRequestingLoan(this)
  }

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusRequestingLoan ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_LOAN_IN_PROGRESS
}
