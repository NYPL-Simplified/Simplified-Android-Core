package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.BookID

/**
 * The type of writable book registries.
 */

interface BookRegistryType : BookRegistryReadableType {

  /**
   * Unconditionally update the status of the given book.
   */

  fun update(status: BookWithStatus)

  /**
   * Atomically update the status of the given book if the current value == the expected value.
   *
   * @return true if successful. False return indicates that the actual value was not equal to the expected value.
   */

  fun compareAndUpdate(expect: BookWithStatus, update: BookWithStatus): Boolean

  /**
   * Conditionally update the status of the given book; the status is only updated if the
   * status is more important according to the priority ordering.
   *
   * @see [BookStatusPriorityOrdering]
   */

  fun updateIfStatusIsMoreImportant(status: BookWithStatus)

  /**
   * Clear the book registry.
   */

  fun clear()

  /**
   * Remove a specific book from the registry.
   */

  fun clearFor(id: BookID)
}
