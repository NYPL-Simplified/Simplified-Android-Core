package org.nypl.simplified.opds2.irradia.internal

import one.irradia.opds2_0.api.OPDS20ParseError
import one.irradia.opds2_0.api.OPDS20ParseResult
import one.irradia.opds2_0.api.OPDS20ParseWarning
import one.irradia.opds2_0.parser.api.OPDS20FeedParserType
import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.parser.api.ParserType
import java.net.URI

internal class OPDS2ParserIrradia(
  private val uri: URI,
  private val parser: OPDS20FeedParserType,
  private val warningsAsErrors: Boolean
) : ParserType<OPDS2Feed> {

  private val warningAsError =
    ParseError(
      source = this.uri,
      message = "One or more warnings were encountered, and we are treating warnings as errors.",
      line = 0,
      column = 0,
      exception = null
    )

  override fun parse(): ParseResult<OPDS2Feed> {
    return when (val result = this.parser.parse()) {
      is OPDS20ParseResult.OPDS20ParseSucceeded ->
        if (this.warningsAsErrors && result.warnings.isNotEmpty()) {
          ParseResult.Failure(
            warnings = this.mapWarnings(result.warnings),
            errors = listOf(this.warningAsError)
          )
        } else {
          ParseResult.Success(
            warnings = this.mapWarnings(result.warnings),
            result = OPDS2IrradiaFeeds.convert(result.result)
          )
        }
      is OPDS20ParseResult.OPDS20ParseFailed ->
        ParseResult.Failure(
          warnings = this.mapWarnings(result.warnings),
          errors = this.mapErrors(result.errors)
        )
    }
  }

  private fun mapWarnings(warnings: List<OPDS20ParseWarning>): List<ParseWarning> {
    return warnings.map(this::mapWarning)
  }

  private fun mapWarning(
    warning: OPDS20ParseWarning
  ): ParseWarning {
    return ParseWarning(
      source = this.uri,
      message = warning.message,
      line = warning.position.line,
      column = warning.position.column,
      exception = warning.exception
    )
  }

  private fun mapErrors(errors: List<OPDS20ParseError>): List<ParseError> {
    return errors.map(this::mapError)
  }

  private fun mapError(
    warning: OPDS20ParseError
  ): ParseError {
    return ParseError(
      source = this.uri,
      message = warning.message,
      line = warning.position.line,
      column = warning.position.column,
      exception = warning.exception
    )
  }

  override fun close() {
    this.parser.close()
  }
}
