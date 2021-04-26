package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionSerializerType
import org.nypl.simplified.links.json.LinkSerialization
import java.io.OutputStream
import java.net.URI

class AccountProviderDescriptionSerializer internal constructor(
  private val uri: URI,
  private val stream: OutputStream,
  private val document: AccountProviderDescription
) : AccountProviderDescriptionSerializerType {

  private val mapper = ObjectMapper()

  override fun serializeToObject(): ObjectNode {
    val metaNode = this.mapper.createObjectNode()
    metaNode.put("updated", this.document.updated.toString())
    metaNode.put("id", this.document.id.toString())
    metaNode.put("title", this.document.title)
    metaNode.put("isProduction", this.document.isProduction)
    metaNode.put("isAutomatic", this.document.isAutomatic)

    this.document.location?.let { location ->
      metaNode.put("location", location.location.toText())
      location.distance?.let { distance ->
        metaNode.put("distance", distance.toText())
      }
    }

    val imagesNode = this.mapper.createArrayNode()
    this.document.images.forEach { link ->
      imagesNode.add(LinkSerialization.serializeLink(link))
    }

    val linksNode = this.mapper.createArrayNode()
    this.document.links.forEach { link ->
      linksNode.add(LinkSerialization.serializeLink(link))
    }

    val objectNode = this.mapper.createObjectNode()
    objectNode.set<ObjectNode>("metadata", metaNode)
    objectNode.set<ArrayNode>("links", linksNode)
    objectNode.set<ArrayNode>("images", imagesNode)
    return objectNode
  }

  override fun serialize() {
    this.mapper.writerWithDefaultPrettyPrinter()
      .writeValue(this.stream, this.serializeToObject())
  }
}
