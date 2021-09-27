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
    val result: T
  ) : ParseResult<T>()

  /**
   * The parse failed.
   */

  data class Failure<T>(
    val warnings: List<ParseWarning>,
    val errors: List<ParseError>
  ) : ParseResult<T>()

  companion object {

    /**
     * Functor map.
     *
     * If r == Success(x), return Success(f(x))
     * If r == Failure(y), return Failure(y)
     */

    fun <A, B> map(
      x: ParseResult<A>,
      f: (A) -> B
    ): ParseResult<B> {
      return when (x) {
        is Success ->
          Success(
            result = f.invoke(x.result),
            warnings = x.warnings
          )
        is Failure ->
          Failure(
            warnings = x.warnings,
            errors = x.errors
          )
      }
    }

    /**
     * Monadic bind.
     *
     * If r == Success(x), return f(r)
     * If r == Failure(y), return Failure(y)
     */

    fun <A, B> flatMap(
      x: ParseResult<A>,
      f: (A) -> ParseResult<B>
    ): ParseResult<B> {
      return when (x) {
        is Success ->
          when (val result = f.invoke(x.result)) {
            is Failure ->
              Failure(
                warnings = result.warnings.plus(x.warnings),
                errors = result.errors
              )
            is Success ->
              Success(
                warnings = result.warnings.plus(x.warnings),
                result = result.result
              )
          }
        is Failure ->
          Failure(
            warnings = x.warnings,
            errors = x.errors
          )
      }
    }

    /**
     * Construct a successful parse result.
     */

    fun <A> succeed(
      warnings: List<ParseWarning>,
      x: A
    ): ParseResult<A> {
      return Success(warnings, x)
    }

    /**
     * Construct a successful parse result.
     */

    fun <A> succeed(
      x: A
    ): ParseResult<A> {
      return Success(listOf(), x)
    }
  }

  /**
   * Functor map.
   * If r == Success(x), return Success(f(x))
   * If r == Failure(y), return Failure(y)
   */

  fun <U> map(f: (T) -> U): ParseResult<U> =
    map(this, f)

  /**
   * Monadic bind.
   * If r == Success(x), return f(r)
   * If r == Failure(y), return Failure(y)
   */

  fun <U> flatMap(f: (T) -> ParseResult<U>): ParseResult<U> =
    flatMap(this, f)
}
