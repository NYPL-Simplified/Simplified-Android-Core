package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.links.Link

/**
 * Functions to serialize links.
 */

object LinkSerialization {

  private val mapper = ObjectMapper()

  /**
   * Serialize a link to a JSON object.
   */

  fun serializeLink(link: Link): ObjectNode {
    val node = this.mapper.createObjectNode()

    link.bitrate?.let { node.put("bitrate", it) }
    link.duration?.let { node.put("duration", it) }
    link.height?.let { node.put("height", it) }
    link.relation?.let { node.put("rel", it) }
    link.title?.let { node.put("title", it) }
    link.type?.let { node.put("type", it.fullType) }
    link.width?.let { node.put("width", it) }

    return when (link) {
      is Link.LinkBasic -> {
        node.put("href", link.href.toString())
        node
      }
      is Link.LinkTemplated -> {
        node.put("href", link.href)
        node.put("templated", true)
        node
      }
    }
  }
}
