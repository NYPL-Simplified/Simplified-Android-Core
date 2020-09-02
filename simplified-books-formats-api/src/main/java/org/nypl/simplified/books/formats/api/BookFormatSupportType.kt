package org.nypl.simplified.books.formats.api

import one.irradia.mime.api.MIMEType

/**
 * Information about the book formats supported by the current application.
 */

interface BookFormatSupportType {

  /**
   * @return `true` if the given MIME type represents a format that can be saved into
   *          the book database
   */

  fun isSupportedFinalContentType(
    mime: MIMEType
  ): Boolean

  /**
   * @return `true` if the given series of MIME types represents an acquisition path that the
   *         current application configuration supports
   */

  fun isSupportedPath(
    typePath: List<MIMEType>
  ): Boolean
}
