package org.nypl.simplified.opds2.r2.internal

import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParserType
import org.readium.r2.opds.OPDS2Parser
import java.io.InputStream
import java.net.URI

class OPDS2ParserR2(
  private val uri: URI,
  private val stream: InputStream,
  private val warningsAsErrors: Boolean
) : ParserType<OPDS2Feed> {

  override fun parse(): ParseResult<OPDS2Feed> {
    return try {
      val data = OPDS2Parser.parse(stream.readBytes(), uri.toURL())
      ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(
          ParseError(
            source = this.uri,
            message = "Not implemented yet!",
            line = 0,
            column = 0,
            exception = null
          )
        )
      )
    } catch (e: Exception) {
      ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(
          ParseError(
            source = this.uri,
            message = e.message ?: e.javaClass.name,
            line = 0,
            column = 0,
            exception = e
          )
        )
      )
    }
  }

  override fun close() {
    this.stream.close()
  }
}
