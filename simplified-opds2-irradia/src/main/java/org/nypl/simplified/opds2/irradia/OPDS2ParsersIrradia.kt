package org.nypl.simplified.opds2.irradia

import one.irradia.opds2_0.parser.extension.library_simplified.OPDS20CatalogExtension
import one.irradia.opds2_0.parser.vanilla.OPDS20FeedParserExtensions
import one.irradia.opds2_0.parser.vanilla.OPDS20FeedParsers
import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.opds2.irradia.internal.OPDS2ParserIrradia
import org.nypl.simplified.opds2.parser.api.OPDS2ParsersType
import org.nypl.simplified.parser.api.ParserType
import java.io.InputStream
import java.net.URI

object OPDS2ParsersIrradia : OPDS2ParsersType {

  private val parsers =
    OPDS20FeedParsers.createWithExtensions(
      extensions = OPDS20FeedParserExtensions(
        feedRoleExtensions = listOf(OPDS20CatalogExtension())
      )
    )

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): ParserType<OPDS2Feed> {
    return OPDS2ParserIrradia(
      uri = uri,
      parser = this.parsers.createParser(uri, stream),
      warningsAsErrors = warningsAsErrors
    )
  }
}
