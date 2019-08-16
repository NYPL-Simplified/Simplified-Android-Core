package org.nypl.simplified.books.controller.api

import org.nypl.simplified.mime.MIMEType

/**
 * An exception indicating that a book cannot be fulfilled because the server
 * delivered an unexpected content type.
 *
 * @param message  The exception message
 * @param expected The expected types
 * @param received The received type
 */

class BookUnexpectedTypeException(
  message: String,
  val expected: Set<MIMEType>,
  val received: MIMEType)
  : BookException(message)
