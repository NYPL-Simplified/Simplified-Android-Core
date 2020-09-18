package org.nypl.simplified.parser.api

import java.io.Serializable
import java.lang.Exception
import java.net.URI

/**
 * An error in a document.
 */

data class ParseError(
  val source: URI,
  val message: String,
  val line: Int = 0,
  val column: Int = 0,
  val exception: Exception? = null
) : Serializable {

  /**
   * Convert a parse error to a warning.
   */

  fun toWarning(): ParseWarning =
    ParseWarning(
      source = this.source,
      message = this.message,
      line = this.line,
      column = this.column,
      exception = this.exception
    )
}
