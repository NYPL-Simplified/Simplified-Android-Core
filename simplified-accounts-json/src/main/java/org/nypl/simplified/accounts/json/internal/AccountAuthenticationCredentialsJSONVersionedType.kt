package org.nypl.simplified.accounts.json.internal

import com.fasterxml.jackson.databind.JsonNode
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.json.core.JSONParseException

/**
 * A versioned JSON serializer.
 */

interface AccountAuthenticationCredentialsJSONVersionedType {

  /**
   * The supported format version.
   */

  val supportedVersion: Int

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   * @return Account credentials
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    node: JsonNode
  ): AccountAuthenticationCredentials
}
