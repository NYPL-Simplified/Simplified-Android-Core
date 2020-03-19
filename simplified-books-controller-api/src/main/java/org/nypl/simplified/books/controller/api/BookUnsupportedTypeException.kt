package org.nypl.simplified.books.controller.api

import one.irradia.mime.api.MIMEType

/**
 * An exception indicating that a book cannot be fulfilled because of an
 * unsupported type.
 */

class BookUnsupportedTypeException(
  val type: MIMEType
) : BookException(type.fullType)
