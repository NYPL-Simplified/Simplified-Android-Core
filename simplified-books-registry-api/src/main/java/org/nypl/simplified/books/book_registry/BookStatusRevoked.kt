package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookID

/**
 * The given book is revoked, but has not yet been removed from the database. A given
 * book is expected to spend very little time in this state.
 */

data class BookStatusRevoked(val id: BookID) : BookStatusType {

  override fun getID(): BookID =
    this.id

  override fun getPriority(): BookStatusPriorityOrdering =
    BookStatusPriorityOrdering.BOOK_STATUS_LOANED

  override fun <A, E : Exception> matchBookStatus(m: BookStatusMatcherType<A, E>): A =
    m.onBookStatusRevoked(this)

  override fun toString(): String {
    val b = StringBuilder(128)
    b.append("[BookStatusRevoked ")
    b.append(this.id)
    b.append("]")
    return b.toString()
  }
}
