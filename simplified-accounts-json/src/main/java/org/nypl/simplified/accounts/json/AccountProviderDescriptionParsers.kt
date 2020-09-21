package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionParsersType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.parser.api.ParserType
import java.io.InputStream
import java.net.URI

/**
 * A provider of account description parsers.
 *
 * Note: MUST have a no-argument public constructor for use in [java.util.ServiceLoader].
 */

class AccountProviderDescriptionParsers : AccountProviderDescriptionParsersType {

  override fun createParserForObject(
    uri: URI,
    objectNode: ObjectNode,
    warningsAsErrors: Boolean
  ): ParserType<AccountProviderDescription> {
    return AccountProviderDescriptionParser(
      uri = uri,
      objectNode = { objectNode },
      warningsAsErrors = warningsAsErrors
    )
  }

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): ParserType<AccountProviderDescription> {
    return AccountProviderDescriptionParser(
      uri = uri,
      objectNode = { JSONParserUtilities.checkObject(null, ObjectMapper().readTree(stream)) },
      warningsAsErrors = warningsAsErrors
    )
  }
}
