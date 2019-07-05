package org.nypl.simplified.accounts.api

import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.parser.api.SerializerType

/**
 * A provider of account provider description serializers.
 */

interface AccountProviderDescriptionSerializerType : SerializerType {

  /**
   * Ignore the given output stream and serialize to a Jackson object node instead.
   */

  fun serializeToObject(): ObjectNode

}
