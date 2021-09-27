package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID

/**
 * The type of book status events.
 */

sealed class BookStatusEvent : BookEvent() {

  /**
   * @return The ID of the book in question
   */

  abstract val bookId: BookID

  /**
   * The previous book status.
   */

  abstract val statusPrevious: BookStatus?

  /**
   * The current book status.
   */

  abstract val statusNow: BookStatus?

  /**
   * A new book status was added to the registry.
   */

  data class BookStatusEventAdded(
    override val bookId: BookID,
    override val statusNow: BookStatus,
  ) : BookStatusEvent() {
    override val statusPrevious: BookStatus? =
      null
  }

  /**
   * The book status was removed from the registry.
   */

  data class BookStatusEventRemoved(
    override val bookId: BookID,
    override val statusPrevious: BookStatus,
  ) : BookStatusEvent() {
    override val statusNow: BookStatus? =
      null
  }

  /**
   * The book status was changed in the registry.
   */

  data class BookStatusEventChanged(
    override val bookId: BookID,
    override val statusPrevious: BookStatus?,
    override val statusNow: BookStatus
  ) : BookStatusEvent()
}
