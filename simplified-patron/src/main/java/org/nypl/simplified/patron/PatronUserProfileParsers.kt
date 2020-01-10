package org.nypl.simplified.patron

import com.fasterxml.jackson.databind.ObjectMapper
import org.nypl.simplified.patron.api.PatronUserProfileParserType
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import java.io.InputStream
import java.net.URI

/**
 * The default provider of patron user profile parsers.
 */

class PatronUserProfileParsers : PatronUserProfileParsersType {

  private val mapper = ObjectMapper()

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): PatronUserProfileParserType =
    PatronUserProfileParser(this.mapper, uri, stream, warningsAsErrors)
}
