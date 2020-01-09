package org.nypl.simplified.accounts.api

import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.parser.api.ParserProviderType
import org.nypl.simplified.parser.api.ParserType
import java.net.URI

/**
 * A provider of account provider description parsers.
 */

interface AccountProviderDescriptionParsersType : ParserProviderType<AccountProviderDescriptionMetadata> {

  fun createParserForObject(
    uri: URI,
    objectNode: ObjectNode,
    warningsAsErrors: Boolean = false
  ): ParserType<AccountProviderDescriptionMetadata>
}
