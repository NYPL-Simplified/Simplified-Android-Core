package org.nypl.simplified.accounts.json

import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionSerializersType
import org.nypl.simplified.parser.api.SerializerType
import java.io.OutputStream
import java.net.URI

/**
 * A provider of account description collection parsers.
 *
 * Note: MUST have a no-argument public constructor for use in [java.util.ServiceLoader].
 */

class AccountProviderDescriptionCollectionSerializers : AccountProviderDescriptionCollectionSerializersType {

  private val serializers = AccountProviderDescriptionSerializers()

  override fun createSerializer(
    uri: URI,
    stream: OutputStream,
    document: AccountProviderDescriptionCollection
  ): SerializerType {
    return AccountProviderDescriptionCollectionSerializer(
      uri = uri,
      stream = stream,
      document = document,
      serializers = this.serializers
    )
  }
}
