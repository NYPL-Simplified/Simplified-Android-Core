package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookID

/**
 * The given book is being returned.
 */

data class BookStatusRequestingRevoke(
  val id: BookID,
  val status: String) : BookStatusType {

  override fun getID(): BookID {
    return this.id
  }

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A {
    return m.onBookStatusRequestingRevoke(this)
  }

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusRequestingRevoke ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }

  override fun getPriority(): BookStatusPriorityOrdering {
    return BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_IN_PROGRESS
  }
}
