package org.nypl.simplified.books.controller.api

import org.nypl.simplified.mime.MIMEType

/**
 * An exception indicating that a book cannot be fulfilled because of an
 * unsupported type.
 */

class BookUnsupportedTypeException(val type: MIMEType) : BookException(type.fullType)
