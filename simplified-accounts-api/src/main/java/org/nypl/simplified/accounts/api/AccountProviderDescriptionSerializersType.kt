package org.nypl.simplified.accounts.api

import org.nypl.simplified.parser.api.SerializersType
import java.io.OutputStream
import java.net.URI

/**
 * A provider of account provider description serializers.
 */

interface AccountProviderDescriptionSerializersType : SerializersType<AccountProviderDescriptionMetadata> {

  override fun createSerializer(
    uri: URI,
    stream: OutputStream,
    document: AccountProviderDescriptionMetadata
  ): AccountProviderDescriptionSerializerType

}
