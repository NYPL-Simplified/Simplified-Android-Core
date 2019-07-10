package org.nypl.simplified.parser.api

import java.io.Closeable

/**
 * A parser.
 */

interface ParserType<T> : Closeable {

  /**
   * Evaluate the parser.
   */

  fun parse(): ParseResult<T>

}