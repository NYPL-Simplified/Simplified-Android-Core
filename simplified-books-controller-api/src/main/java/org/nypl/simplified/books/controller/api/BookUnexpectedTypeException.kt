package org.nypl.simplified.books.controller.api

/**
 * An exception indicating that a book cannot be fulfilled because the server
 * delivered an unexpected content type.
 *
 * @param message The exception message
 * @param expected The expected types
 * @param received The received type
 */

class BookUnexpectedTypeException(
  message: String,
  val expected: Set<String>,
  val received: String
) : BookException(message)
