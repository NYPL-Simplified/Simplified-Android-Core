package org.nypl.simplified.accounts.json

import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionSerializerType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionSerializersType
import java.io.OutputStream
import java.net.URI

/**
 * A provider of account description serializers.
 *
 * Note: MUST have a no-argument public constructor for use in [java.util.ServiceLoader].
 */

class AccountProviderDescriptionSerializers : AccountProviderDescriptionSerializersType {

  override fun createSerializer(
    uri: URI,
    stream: OutputStream,
    document: AccountProviderDescription
  ): AccountProviderDescriptionSerializerType {
    return AccountProviderDescriptionSerializer(uri, stream, document)
  }
}
