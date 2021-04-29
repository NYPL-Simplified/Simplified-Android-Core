package org.nypl.simplified.opds2.r2

import org.nypl.simplified.opds2.OPDS2Feed
import org.nypl.simplified.opds2.parser.api.OPDS2ParsersType
import org.nypl.simplified.opds2.r2.internal.OPDS2ParserR2
import org.nypl.simplified.parser.api.ParserType
import java.io.InputStream
import java.net.URI

object OPDS2ParsersR2 : OPDS2ParsersType {

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): ParserType<OPDS2Feed> {
    return OPDS2ParserR2(
      uri = uri,
      stream = stream,
      warningsAsErrors = warningsAsErrors
    )
  }
}
