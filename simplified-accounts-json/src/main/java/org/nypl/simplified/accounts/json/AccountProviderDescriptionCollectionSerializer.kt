package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionSerializerType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionSerializersType
import org.nypl.simplified.links.json.LinkSerialization
import java.io.OutputStream
import java.net.URI

/**
 * A serializer of provider description collections.
 */

class AccountProviderDescriptionCollectionSerializer internal constructor(
  private val uri: URI,
  private val stream: OutputStream,
  private val document: AccountProviderDescriptionCollection,
  private val serializers: AccountProviderDescriptionSerializersType
) : AccountProviderDescriptionCollectionSerializerType {

  private val mapper = ObjectMapper()

  override fun serialize() {
    val objectNode = this.mapper.createObjectNode()
    objectNode.set<ArrayNode>("catalogs", this.serializeCatalogs())
    objectNode.set<ObjectNode>("metadata", this.serializeMetadata())
    objectNode.set<ArrayNode>("links", this.serializeLinksNode())
    this.mapper.writerWithDefaultPrettyPrinter().writeValue(this.stream, objectNode)
  }

  private fun serializeLinksNode(): ArrayNode {
    val arrayNode = this.mapper.createArrayNode()
    this.document.links.forEach { link ->
      arrayNode.add(LinkSerialization.serializeLink(link))
    }
    return arrayNode
  }

  private fun serializeMetadata(): ObjectNode {
    val objectNode = this.mapper.createObjectNode()
    objectNode.put("title", this.document.metadata.title)
    return objectNode
  }

  private fun serializeCatalogs(): ArrayNode {
    val catalogsNode = this.mapper.createArrayNode()
    for (provider in this.document.providers) {
      val serializer =
        this.serializers.createSerializer(this.uri, IgnoreStream(), provider)
      catalogsNode.add(serializer.serializeToObject())
    }
    return catalogsNode
  }

  private class IgnoreStream : OutputStream() {
    override fun write(b: Int) {
    }
  }
}
