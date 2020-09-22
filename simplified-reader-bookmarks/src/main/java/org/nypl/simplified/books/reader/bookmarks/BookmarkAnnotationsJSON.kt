package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationBodyNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationFirstNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationResponse
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationSelectorNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationTargetNode

object BookmarkAnnotationsJSON {

  @Throws(JSONParseException::class)
  fun deserializeSelectorNodeFromJSON(node: ObjectNode): BookmarkAnnotationSelectorNode {
    return BookmarkAnnotationSelectorNode(
      type = JSONParserUtilities.getString(node, "type"),
      value = JSONParserUtilities.getString(node, "value")
    )
  }

  fun serializeSelectorNodeToJSON(
    mapper: ObjectMapper,
    selector: BookmarkAnnotationSelectorNode
  ): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("type", selector.type)
    node.put("value", selector.value)
    return node
  }

  fun serializeTargetNodeToJSON(
    mapper: ObjectMapper,
    target: BookmarkAnnotationTargetNode
  ): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("source", target.source)
    node.set<ObjectNode>("selector", serializeSelectorNodeToJSON(mapper, target.selector))
    return node
  }

  @Throws(JSONParseException::class)
  fun deserializeTargetNodeFromJSON(node: ObjectNode): BookmarkAnnotationTargetNode {
    return BookmarkAnnotationTargetNode(
      source = JSONParserUtilities.getString(node, "source"),
      selector = deserializeSelectorNodeFromJSON(JSONParserUtilities.getObject(node, "selector"))
    )
  }

  fun serializeBodyNodeToJSON(
    mapper: ObjectMapper,
    target: BookmarkAnnotationBodyNode
  ): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("http://librarysimplified.org/terms/time", target.timestamp)
    node.put("http://librarysimplified.org/terms/device", target.device)

    target.chapterTitle.let { v ->
      node.put("http://librarysimplified.org/terms/chapter", v)
    }
    target.chapterProgress.let { v ->
      node.put("http://librarysimplified.org/terms/progressWithinChapter", v)
    }
    target.bookProgress.let { v ->
      node.put("http://librarysimplified.org/terms/progressWithinBook", v)
    }
    return node
  }

  private fun <T> mapOptionNull(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }

  @Throws(JSONParseException::class)
  fun deserializeBodyNodeFromJSON(node: ObjectNode): BookmarkAnnotationBodyNode {
    return BookmarkAnnotationBodyNode(
      timestamp =
        JSONParserUtilities.getString(node, "http://librarysimplified.org/terms/time"),
      device =
        JSONParserUtilities.getString(node, "http://librarysimplified.org/terms/device"),
      chapterTitle =
        mapOptionNull(
          JSONParserUtilities.getStringOptional(node, "http://librarysimplified.org/terms/chapter")
        ),
      chapterProgress =
        mapOptionNull(
          JSONParserUtilities.getDoubleOptional(node, "http://librarysimplified.org/terms/progressWithinChapter")
            .map { x -> x.toFloat() }
        ),
      bookProgress =
        mapOptionNull(
          JSONParserUtilities.getDoubleOptional(node, "http://librarysimplified.org/terms/progressWithinBook")
            .map { x -> x.toFloat() }
        )
    )
  }

  fun serializeBookmarkAnnotationToJSON(
    mapper: ObjectMapper,
    annotation: BookmarkAnnotation
  ): ObjectNode {
    val node = mapper.createObjectNode()
    if (annotation.context != null) { node.put("@context", annotation.context) }
    if (annotation.id != null) { node.put("id", annotation.id) }
    node.put("motivation", annotation.motivation)
    node.put("type", annotation.type)
    node.set<ObjectNode>("target", serializeTargetNodeToJSON(mapper, annotation.target))
    node.set<ObjectNode>("body", serializeBodyNodeToJSON(mapper, annotation.body))
    return node
  }

  fun serializeBookmarkAnnotationToBytes(
    mapper: ObjectMapper,
    annotation: BookmarkAnnotation
  ): ByteArray {
    return mapper.writeValueAsBytes(serializeBookmarkAnnotationToJSON(mapper, annotation))
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationFromJSON(node: ObjectNode): BookmarkAnnotation {
    return BookmarkAnnotation(
      context =
        mapOptionNull(JSONParserUtilities.getStringOptional(node, "@context")),
      body =
        deserializeBodyNodeFromJSON(JSONParserUtilities.getObject(node, "body")),
      id =
        mapOptionNull(JSONParserUtilities.getStringOptional(node, "id")),
      type =
        JSONParserUtilities.getString(node, "type"),
      motivation =
        JSONParserUtilities.getString(node, "motivation"),
      target =
        deserializeTargetNodeFromJSON(JSONParserUtilities.getObject(node, "target"))
    )
  }

  fun serializeBookmarkAnnotationFirstNodeToJSON(
    mapper: ObjectMapper,
    annotation: BookmarkAnnotationFirstNode
  ): ObjectNode {
    val nodes = mapper.createArrayNode()
    annotation.items.forEach { mark -> nodes.add(serializeBookmarkAnnotationToJSON(mapper, mark)) }

    val node = mapper.createObjectNode()
    node.put("id", annotation.id)
    node.put("type", annotation.type)
    node.set<ArrayNode>("items", nodes)
    return node
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationFirstNodeFromJSON(node: ObjectNode): BookmarkAnnotationFirstNode {
    return BookmarkAnnotationFirstNode(
      type = JSONParserUtilities.getString(node, "type"),
      id = JSONParserUtilities.getString(node, "id"),
      items = JSONParserUtilities.getArray(node, "items").map { items ->
        deserializeBookmarkAnnotationFromJSON(JSONParserUtilities.checkObject(null, items))
      }
    )
  }

  fun serializeBookmarkAnnotationResponseToJSON(
    mapper: ObjectMapper,
    annotation: BookmarkAnnotationResponse
  ): ObjectNode {
    val node = mapper.createObjectNode()
    node.put("total", annotation.total)
    node.put("id", annotation.id)
    node.set<ArrayNode>("@context", serializeStringArray(mapper, annotation.context))
    node.set<ArrayNode>("type", serializeStringArray(mapper, annotation.type))
    node.set<ObjectNode>("first", serializeBookmarkAnnotationFirstNodeToJSON(mapper, annotation.first))
    return node
  }

  private fun serializeStringArray(
    mapper: ObjectMapper,
    context: List<String>
  ): ArrayNode {
    val array = mapper.createArrayNode()
    context.forEach { text -> array.add(text) }
    return array
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationResponseFromJSON(node: ObjectNode): BookmarkAnnotationResponse {
    return BookmarkAnnotationResponse(
      context =
        JSONParserUtilities.getArray(node, "@context")
          .map { v -> JSONParserUtilities.checkString(v) },
      total =
        JSONParserUtilities.getInteger(node, "total"),
      type =
        JSONParserUtilities.getArray(node, "type")
          .map { v -> JSONParserUtilities.checkString(v) },
      id =
        JSONParserUtilities.getString(node, "id"),
      first =
        deserializeBookmarkAnnotationFirstNodeFromJSON(JSONParserUtilities.getObject(node, "first"))
    )
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationResponseFromJSON(node: JsonNode): BookmarkAnnotationResponse {
    return deserializeBookmarkAnnotationResponseFromJSON(
      JSONParserUtilities.checkObject(null, node)
    )
  }
}
