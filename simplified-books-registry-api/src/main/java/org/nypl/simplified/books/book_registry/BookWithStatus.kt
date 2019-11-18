package org.nypl.simplified.books.book_registry

import org.nypl.simplified.books.api.Book

/**
 * A book with an associated status value.
 */

data class BookWithStatus(
  val book: Book,
  val status: BookStatus)
