package org.nypl.simplified.mime

/**
 * The result of lexing a token from a stream.
 */

sealed class MIMELexerResult {

  /**
   * The position of the result.
   */

  abstract val position: Int

  /**
   * Lexing succeeded. The given token is the current token in the stream.
   */

  data class Success(
    val token: MIMEToken)
    : MIMELexerResult() {
    override val position = this.token.position
  }

  /**
   * Lexing failed. The given exception describes the error.
   */

  data class Failure(
    override val position: Int,
    val exception: Exception)
    : MIMELexerResult()

}
