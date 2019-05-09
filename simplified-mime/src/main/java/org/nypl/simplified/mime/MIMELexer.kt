package org.nypl.simplified.mime

import org.nypl.simplified.mime.MIMEToken.MIMETextToken.Quoted
import org.nypl.simplified.mime.MIMEToken.MIMETextToken.Text
import java.util.Deque
import java.util.LinkedList

/**
 * A lexer for RFC2045 MIME types.
 *
 * This implementation is significantly more lax than the RFC2045 specification in that parameter
 * names and values are allowed to contain anything that isn't a semicolon. Quoted strings may
 * contain any character except for '"' and there is no escape mechanism.
 */

class MIMELexer private constructor(
  private val codepoints: Array<Int>,
  private var positionCurrent: Int = 0,
  private var positionStart: Int = 0,
  private val buffer: StringBuilder = java.lang.StringBuilder(64),
  private var stateLexers: Deque<PartialLexerType> = LinkedList()) : MIMELexerType {

  override val position: Int
    get() = this.positionCurrent

  init {
    this.stateLexers.push(this.TextLexer())
  }

  private fun nextCodepoint(): Int? {
    if (this.positionCurrent >= this.codepoints.size) {
      return null
    }
    val codepoint = this.codepoints[this.positionCurrent]
    this.positionCurrent = this.positionCurrent + 1
    return codepoint
  }

  private fun pushBackCodepoint() {
    this.positionCurrent = Math.max(0, this.positionCurrent - 1)
  }

  private interface PartialLexerType {

    @Throws(LexerException::class)
    fun handleCodePoint(code: Int?): MIMEToken?

  }

  private abstract class LexerException(message: String) : Exception(message)

  private class LexerUnexpectedEOF : LexerException("Unexpected EOF inside quoted string")

  /**
   * A lexer operating in quoted strings.
   */

  private inner class QuoteLexer : PartialLexerType {

    @Throws(LexerException::class)
    override fun handleCodePoint(code: Int?): MIMEToken? {
      return when (code) {
        null ->
          throw LexerUnexpectedEOF()

        '"'.toInt() -> {
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Quoted(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            this@MIMELexer.stateLexers.pop()
            null
          }
        }

        else -> {
          this@MIMELexer.buffer.appendCodePoint(code)
          return null
        }
      }
    }
  }

  /**
   * A lexer operating in the initial section of MIME type strings.
   */

  private inner class TextLexer : PartialLexerType {

    @Throws(LexerException::class)
    override fun handleCodePoint(code: Int?): MIMEToken? {
      return when (code) {
        null ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            this@MIMELexer.updateStartPosition()
            MIMEToken.EOF(this@MIMELexer.positionStart)
          }

        '/'.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            val token = MIMEToken.Slash(this@MIMELexer.positionStart)
            this@MIMELexer.updateStartPosition()
            token
          }

        ';'.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            this@MIMELexer.stateLexers.push(ParameterLexer())
            token
          } else {
            val token = MIMEToken.Semicolon(this@MIMELexer.positionStart)
            this@MIMELexer.updateStartPosition()
            this@MIMELexer.stateLexers.push(ParameterLexer())
            token
          }

        '='.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            val token = MIMEToken.Equals(this@MIMELexer.positionStart)
            this@MIMELexer.updateStartPosition()
            token
          }

        '"'.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            return token
          } else {
            this@MIMELexer.updateStartPosition()
            this@MIMELexer.stateLexers.push(this@MIMELexer.QuoteLexer())
            null
          }

        else -> {
          if (!Character.isWhitespace(code)) {
            this@MIMELexer.buffer.appendCodePoint(code)
          }
          return null
        }
      }
    }
  }

  /**
   * A lexer operating in the parameter section of MIME type strings.
   */

  private inner class ParameterLexer : PartialLexerType {

    @Throws(LexerException::class)
    override fun handleCodePoint(code: Int?): MIMEToken? {
      return when (code) {
        null ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            this@MIMELexer.updateStartPosition()
            MIMEToken.EOF(this@MIMELexer.positionStart)
          }

        ';'.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            val token = MIMEToken.Semicolon(this@MIMELexer.positionStart)
            this@MIMELexer.updateStartPosition()
            token
          }

        '='.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            token
          } else {
            val token = MIMEToken.Equals(this@MIMELexer.positionStart)
            this@MIMELexer.updateStartPosition()
            token
          }

        '"'.toInt() ->
          if (!this@MIMELexer.buffer.isEmpty()) {
            this@MIMELexer.pushBackCodepoint()
            val text = this@MIMELexer.finishText()
            val token = Text(this@MIMELexer.positionStart, text)
            this@MIMELexer.updateStartPosition()
            return token
          } else {
            this@MIMELexer.updateStartPosition()
            this@MIMELexer.stateLexers.push(this@MIMELexer.QuoteLexer())
            null
          }

        else -> {
          if (!Character.isWhitespace(code)) {
            this@MIMELexer.buffer.appendCodePoint(code)
          }
          return null
        }
      }
    }
  }

  private fun updateStartPosition() {
    this@MIMELexer.positionStart = this@MIMELexer.positionCurrent
  }

  private fun finishText(): String {
    val text = this.buffer.toString()
    this.buffer.setLength(0)
    return text
  }

  override fun token(): MIMELexerResult {
    while (true) {
      try {
        val code = this.nextCodepoint()
        val lexer = this.stateLexers.peek()
        val token = lexer.handleCodePoint(code)
        if (token != null) {
          return MIMELexerResult.Success(token)
        }
      } catch (e: Exception) {
        return MIMELexerResult.Failure(this.positionCurrent, e)
      }
    }
  }

  companion object {

    /**
     * Create a new lexer.
     */

    fun create(text: String): MIMELexerType {
      val count = text.codePointCount(0, text.lastIndex + 1)
      val codepoints = Array(count, { 0 })
      for (index in 0 until count) {
        codepoints[index] = text.codePointAt(index)
      }

      return MIMELexer(codepoints = codepoints)
    }

  }
}