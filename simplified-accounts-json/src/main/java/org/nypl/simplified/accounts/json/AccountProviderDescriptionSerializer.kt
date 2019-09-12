package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionSerializerType
import java.io.OutputStream
import java.net.URI

class AccountProviderDescriptionSerializer internal constructor(
  private val uri: URI,
  private val stream: OutputStream,
  private val document: AccountProviderDescriptionMetadata
) : AccountProviderDescriptionSerializerType {

  private val mapper = ObjectMapper()

  override fun serializeToObject(): ObjectNode {
    val metaNode = this.mapper.createObjectNode()
    metaNode.put("updated", this.document.updated.toString())
    metaNode.put("id", this.document.id.toString())
    metaNode.put("title", this.document.title)
    metaNode.put("isProduction", this.document.isProduction)
    metaNode.put("isAutomatic", this.document.isAutomatic)

    val imagesNode = this.mapper.createArrayNode()
    this.document.images.forEach { link ->
      imagesNode.add(LinkSerialization.serializeLink(link))
    }

    val linksNode = this.mapper.createArrayNode()
    this.document.links.forEach { link ->
      linksNode.add(LinkSerialization.serializeLink(link))
    }

    val objectNode = this.mapper.createObjectNode()
    objectNode.set("metadata", metaNode)
    objectNode.set("links", linksNode)
    objectNode.set("images", imagesNode)
    return objectNode
  }

  override fun serialize() {
    this.mapper.writerWithDefaultPrettyPrinter()
      .writeValue(this.stream, this.serializeToObject())
  }
}
