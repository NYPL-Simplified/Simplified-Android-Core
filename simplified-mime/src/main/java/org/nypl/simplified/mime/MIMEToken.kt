package org.nypl.simplified.mime

import org.nypl.simplified.mime.MIMEToken.MIMETextToken.Quoted
import org.nypl.simplified.mime.MIMEToken.MIMETextToken.Text

/**
 * The type of tokens that can be lexed from RFC2045 MIME types.
 */

sealed class MIMEToken {

  abstract val position: Int

  abstract val description : String

  /**
   * The type of text tokens.
   */

  sealed class MIMETextToken : MIMEToken() {

    abstract val text : String

    /**
     * A simple text identifier.
     */

    data class Text(
      override val position: Int,
      override val text: String)
      : MIMETextToken() {

      override val description: String
        get() = "A text token"
    }

    /**
     * A quoted string.
     */

    data class Quoted(
      override val position: Int,
      override val text: String)
      : MIMETextToken() {

      override val description: String
        get() = "A quoted string"
    }
  }

  /**
   * A forward slash. Typically used to separate types and subtypes.
   */

  data class Slash(
    override val position: Int)
    : MIMEToken() {

    override val description: String
      get() = "A forward slash: '/'"
  }

  /**
   * A semicolon. Used to separate parameter/value pairs.
   */

  data class Semicolon(
    override val position: Int)
    : MIMEToken() {

    override val description: String
      get() = "A semicolon: ';'"
  }

  /**
   * An equals character. Used to combine parameter value pairs.
   */

  data class Equals(
    override val position: Int)
    : MIMEToken() {

    override val description: String
      get() = "An equals symbol: '='"
  }

  /**
   * End-of-stream.
   */

  data class EOF(
    override val position: Int)
    : MIMEToken() {

    override val description: String
      get() = "End-of-stream"
  }

  companion object {

    fun <T : MIMEToken> describe(clazz: Class<T>): String {
      return when (clazz) {
        MIMETextToken::class.java ->
          MIMETextToken::class.sealedSubclasses
            .joinToString(separator = " | ", transform = { type -> describe(type.java) })

        Text::class.java ->
          Text(0, "text").description
        Quoted::class.java ->
          Quoted(0, "text").description
        Slash::class.java ->
          Slash(0).description
        Semicolon::class.java ->
          Semicolon(0).description
        Equals::class.java ->
          Equals(0).description
        EOF::class.java ->
          EOF(0).description
        else ->
          throw IllegalStateException("Unrecognized token type: " + clazz)
      }
    }
  }

}
