package org.nypl.simplified.mime

import org.nypl.simplified.mime.MIMEToken.MIMETextToken
import org.nypl.simplified.mime.MIMEToken.MIMETextToken.Text
import org.nypl.simplified.mime.MIMEToken.Semicolon
import org.nypl.simplified.mime.MIMEToken.Slash
import kotlin.reflect.KClass

/**
 * A parser for RFC2045 MIME types.
 */

class MIMEParser private constructor(
  private val text: String,
  private val lexer: MIMELexerType
) : MIMEParserType {

  override fun parse(): MIMEParserResult {
    try {
      val type = takeExact(Text::class)
      takeExact(Slash::class)
      val subtype = takeExact(Text::class)

      val semi0 = takeOptionalExact(Semicolon::class)
      if (semi0 == null) {
        return MIMEParserResult.Success(
          0,
          MIMEType(type = type.text, subtype = subtype.text, parameters = mapOf()))
      }

      val parameters = mutableMapOf<String, String>()
      while (true) {
        val name = takeTextTokenOptional()
        if (name == null) {
          return MIMEParserResult.Success(
            0,
            MIMEType(type = type.text, subtype = subtype.text, parameters = parameters.toMap()))
        }

        takeExact(MIMEToken.Equals::class)
        val value = takeTextToken()
        parameters[name.text] = value.text

        val semi1 = takeOptionalExact(Semicolon::class)
        if (semi1 == null) {
          return MIMEParserResult.Success(
            0,
            MIMEType(type = type.text, subtype = subtype.text, parameters = parameters.toMap()))
        }
      }
    } catch (e: Exception) {
      return MIMEParserResult.Failure(this.lexer.position, e)
    }
  }

  private class ParseException(message: String) : java.lang.Exception(message) {
    companion object {
      fun <T : MIMEToken> expectedExactly(
        expected: KClass<T>,
        received: MIMEToken,
        fullText: String
      ): ParseException {
        return ParseException(
          StringBuilder(128)
            .append("Parse error")
            .append('\n')
            .append("  Expected: ")
            .append(MIMEToken.describe(expected.java))
            .append('\n')
            .append("  Received: ")
            .append(received.description)
            .append('\n')
            .append("  Received text: ")
            .append(fullText)
            .append('\n')
            .append("  Position: ")
            .append(received.position)
            .append('\n')
            .toString())
      }
    }
  }

  private fun takeTextToken(): MIMETextToken {
    val result = this.lexer.token()
    return when (result) {
      is MIMELexerResult.Success -> {
        val token = result.token
        if (token is MIMETextToken) {
          token
        } else {
          throw ParseException.expectedExactly(MIMETextToken::class, token, this.text)
        }
      }
      is MIMELexerResult.Failure -> {
        throw result.exception
      }
    }
  }

  private fun takeTextTokenOptional(): MIMETextToken? {
    val result = this.lexer.token()
    return when (result) {
      is MIMELexerResult.Success -> {
        val token = result.token
        when (token) {
          is MIMETextToken -> token
          is MIMEToken.EOF -> null
          else -> {
            throw ParseException.expectedExactly(MIMETextToken::class, token, this.text)
          }
        }
      }
      is MIMELexerResult.Failure -> {
        throw result.exception
      }
    }
  }

  private fun <T : MIMEToken> takeExact(clazz: KClass<T>): T {
    val result = this.lexer.token()
    return when (result) {
      is MIMELexerResult.Success -> {
        val token = result.token
        if (token::class.java.isAssignableFrom(clazz.java)) {
          token as T
        } else {
          throw ParseException.expectedExactly(clazz, token, this.text)
        }
      }
      is MIMELexerResult.Failure -> {
        throw result.exception
      }
    }
  }

  private fun <T : MIMEToken> takeOptionalExact(clazz: KClass<T>): T? {
    val result = this.lexer.token()
    return when (result) {
      is MIMELexerResult.Success -> {
        val token = result.token
        when (token) {
          is MIMEToken.EOF -> null
          else -> {
            if (token.javaClass == clazz.java) {
              token as T
            } else {
              throw ParseException.expectedExactly(clazz, token, this.text)
            }
          }
        }
      }
      is MIMELexerResult.Failure -> {
        throw result.exception
      }
    }
  }

  companion object {

    /**
     * Create a new parser for the given string.
     */

    fun create(text: String): MIMEParserType {
      val lexer = MIMELexer.create(text)
      return MIMEParser(text = text, lexer = lexer)
    }

    /**
     * Create a parser for the given string, run the parser, and return either the successful
     * result of parsing, or raise an exception indicating the failure to parse.
     */

    @Throws(Exception::class)
    fun parseRaisingException(text: String): MIMEType {
      return create(text).parseExceptionally()
    }
  }
}
