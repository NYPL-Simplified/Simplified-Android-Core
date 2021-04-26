package org.nypl.simplified.accounts.json

import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParserType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParsersType
import org.nypl.simplified.opds2.parser.api.OPDS2ParsersType
import java.io.InputStream
import java.net.URI

/**
 * A provider of account description collection parsers.
 */

class AccountProviderDescriptionCollectionParsers(
  private val opdsParsers: OPDS2ParsersType
) : AccountProviderDescriptionCollectionParsersType {

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): AccountProviderDescriptionCollectionParserType {
    return AccountProviderDescriptionCollectionParser(
      this.opdsParsers.createParser(uri, stream, warningsAsErrors)
    )
  }
}
