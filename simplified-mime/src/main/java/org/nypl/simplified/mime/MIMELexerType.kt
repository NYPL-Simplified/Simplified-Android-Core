package org.nypl.simplified.mime

/**
 * The type of lexers for RFC2045 MIME type strings.
 */

interface MIMELexerType {

  /**
   * The current lexer position.
   */

  val position: Int

  /**
   * Return the next token in the stream.
   */

  fun token(): MIMELexerResult

}
