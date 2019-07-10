package org.nypl.simplified.parser.api

/**
 * The result of a parsing operation.
 */

sealed class ParseResult<T> {

  /**
   * The parse succeeded.
   */

  data class Success<T>(
    val warnings: List<ParseWarning>,
    val result: T)
    : ParseResult<T>()

  /**
   * The parse failed.
   */

  data class Failure<T>(
    val warnings: List<ParseWarning>,
    val errors: List<ParseError>)
    : ParseResult<T>()
}
