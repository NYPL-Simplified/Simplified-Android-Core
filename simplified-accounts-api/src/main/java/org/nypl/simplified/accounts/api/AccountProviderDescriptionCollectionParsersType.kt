package org.nypl.simplified.accounts.api

import org.nypl.simplified.parser.api.ParserProviderType
import java.io.InputStream
import java.net.URI

/**
 * A provider of account provider description collection parsers.
 */

interface AccountProviderDescriptionCollectionParsersType : ParserProviderType<AccountProviderDescriptionCollection> {
  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): AccountProviderDescriptionCollectionParserType
}
