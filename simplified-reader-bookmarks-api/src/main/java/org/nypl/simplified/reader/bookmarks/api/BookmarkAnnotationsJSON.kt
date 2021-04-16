package org.nypl.simplified.reader.bookmarks.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.simplified.books.api.BookChapterProgress
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities

object BookmarkAnnotationsJSON {

  @Throws(JSONParseException::class)
  fun deserializeSelectorNodeFromJSON(
    objectMapper: ObjectMapper,
    node: ObjectNode
  ): BookmarkAnnotationSelectorNode {
    val type =
      JSONParserUtilities.getString(node, "type")
    val value =
      JSONParserUtilities.getString(node, "value")

    /*
     * Attempt to deserialize the value as a location in order to check the structure. We
     * don't actually need the parsed value here.
     */

    try {
      this.deserializeLocation(objectMapper, value)
    } catch (e: Exception) {
      throw JSONParseException(e)
    }

    when (type) {
      "FragmentSelector",
      "oa:FragmentSelector" ->
        Unit
      else -> {
        throw JSONParseException("Unrecognized selector node type: $type")
      }
    }

    return BookmarkAnnotationSelectorNode(
      type = type,
      value = value
    )
  }

  fun serializeSelectorNodeToJSON(
    objectMapper: ObjectMapper,
    selector: BookmarkAnnotationSelectorNode
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("type", selector.type)
    node.put("value", selector.value)
    return node
  }

  fun serializeTargetNodeToJSON(
    objectMapper: ObjectMapper,
    target: BookmarkAnnotationTargetNode
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("source", target.source)
    node.set<ObjectNode>("selector", this.serializeSelectorNodeToJSON(objectMapper, target.selector))
    return node
  }

  @Throws(JSONParseException::class)
  fun deserializeTargetNodeFromJSON(
    objectMapper: ObjectMapper,
    node: ObjectNode
  ): BookmarkAnnotationTargetNode {
    return BookmarkAnnotationTargetNode(
      source = JSONParserUtilities.getString(node, "source"),
      selector = this.deserializeSelectorNodeFromJSON(objectMapper, JSONParserUtilities.getObject(node, "selector"))
    )
  }

  fun serializeBodyNodeToJSON(
    mapper: ObjectMapper,
    target: BookmarkAnnotationBodyNode
  ): ObjectNode {
    val node = mapper.createObjectNode()
    target.timestamp?.let { v ->
      node.put("http://librarysimplified.org/terms/time", v)
    }
    target.device?.let { v ->
      node.put("http://librarysimplified.org/terms/device", v)
    }
    target.chapterTitle?.let { v ->
      node.put("http://librarysimplified.org/terms/chapter", v)
    }
    target.bookProgress?.let { v ->
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
        JSONParserUtilities.getStringOrNull(node, "http://librarysimplified.org/terms/chapter"),
      bookProgress =
        this.mapOptionNull(
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
    if (annotation.context != null) {
      node.put("@context", annotation.context)
    }
    if (annotation.id != null) {
      node.put("id", annotation.id)
    }
    node.put("motivation", annotation.motivation)
    node.put("type", annotation.type)
    node.set<ObjectNode>("target", this.serializeTargetNodeToJSON(mapper, annotation.target))
    node.set<ObjectNode>("body", this.serializeBodyNodeToJSON(mapper, annotation.body))
    return node
  }

  fun serializeBookmarkAnnotationToBytes(
    objectMapper: ObjectMapper,
    annotation: BookmarkAnnotation
  ): ByteArray {
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false)
    objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    return objectMapper.writeValueAsBytes(this.serializeBookmarkAnnotationToJSON(objectMapper, annotation))
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationFromJSON(
    objectMapper: ObjectMapper,
    node: ObjectNode
  ): BookmarkAnnotation {
    return BookmarkAnnotation(
      context =
        this.mapOptionNull(JSONParserUtilities.getStringOptional(node, "@context")),
      body =
        this.deserializeBodyNodeFromJSON(JSONParserUtilities.getObject(node, "body")),
      id =
        this.mapOptionNull(JSONParserUtilities.getStringOptional(node, "id")),
      type =
        JSONParserUtilities.getString(node, "type"),
      motivation =
        JSONParserUtilities.getString(node, "motivation"),
      target =
        this.deserializeTargetNodeFromJSON(objectMapper, JSONParserUtilities.getObject(node, "target"))
    )
  }

  fun serializeBookmarkAnnotationFirstNodeToJSON(
    objectMapper: ObjectMapper,
    annotation: BookmarkAnnotationFirstNode
  ): ObjectNode {
    val nodes = objectMapper.createArrayNode()
    annotation.items.forEach { mark -> nodes.add(this.serializeBookmarkAnnotationToJSON(objectMapper, mark)) }

    val node = objectMapper.createObjectNode()
    node.put("id", annotation.id)
    node.put("type", annotation.type)
    node.set<ArrayNode>("items", nodes)
    return node
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationFirstNodeFromJSON(
    objectMapper: ObjectMapper,
    node: ObjectNode
  ): BookmarkAnnotationFirstNode {
    return BookmarkAnnotationFirstNode(
      type = JSONParserUtilities.getString(node, "type"),
      id = JSONParserUtilities.getString(node, "id"),
      items = JSONParserUtilities.getArray(node, "items").map { items ->
        this.deserializeBookmarkAnnotationFromJSON(
          objectMapper = objectMapper,
          node = JSONParserUtilities.checkObject(null, items)
        )
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
    node.set<ArrayNode>("@context", this.serializeStringArray(mapper, annotation.context))
    node.set<ArrayNode>("type", this.serializeStringArray(mapper, annotation.type))
    node.set<ObjectNode>("first", this.serializeBookmarkAnnotationFirstNodeToJSON(mapper, annotation.first))
    return node
  }

  private fun serializeStringArray(
    objectMapper: ObjectMapper,
    context: List<String>
  ): ArrayNode {
    val array = objectMapper.createArrayNode()
    context.forEach { text -> array.add(text) }
    return array
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationResponseFromJSON(
    objectMapper: ObjectMapper,
    node: ObjectNode
  ): BookmarkAnnotationResponse {
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
        this.deserializeBookmarkAnnotationFirstNodeFromJSON(
          objectMapper = objectMapper,
          node = JSONParserUtilities.getObject(node, "first")
        )
    )
  }

  @Throws(JSONParseException::class)
  fun deserializeBookmarkAnnotationResponseFromJSON(
    objectMapper: ObjectMapper,
    node: JsonNode
  ): BookmarkAnnotationResponse {
    return this.deserializeBookmarkAnnotationResponseFromJSON(
      objectMapper = objectMapper,
      node = JSONParserUtilities.checkObject(null, node)
    )
  }

  @Throws(JSONParseException::class)
  fun serializeLocation(
    objectMapper: ObjectMapper,
    location: BookLocation
  ): String {
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false)
    objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    return objectMapper.writeValueAsString(this.serializeLocationToNode(objectMapper, location))
  }

  @Throws(JSONParseException::class)
  private fun serializeLocationToNode(
    objectMapper: ObjectMapper,
    location: BookLocation
  ): ObjectNode {
    val objectNode = objectMapper.createObjectNode()
    return when (location) {
      is BookLocation.BookLocationR2 -> {
        objectNode.put("@type", "LocatorHrefProgression")
        objectNode.put("href", location.progress.chapterHref)
        objectNode.put("progressWithinChapter", location.progress.chapterProgress)
        objectNode
      }
      is BookLocation.BookLocationR1 -> {
        objectNode.put("@type", "LocatorLegacyCFI")
        location.idRef?.let {
          objectNode.put("idref", it)
        }
        location.contentCFI?.let {
          objectNode.put("contentCFI", it)
        }
        objectNode.put("progressWithinChapter", location.progress ?: 0.0)
        objectNode
      }
    }
  }

  @Throws(JSONParseException::class)
  fun deserializeLocation(
    objectMapper: ObjectMapper,
    value: String
  ): BookLocation {
    val node =
      objectMapper.readTree(value)
    val obj =
      JSONParserUtilities.checkObject(null, node)
    val type =
      JSONParserUtilities.getStringOrNull(obj, "@type")

    return when (type) {
      "LocatorHrefProgression" ->
        this.deserializeLocationR2(obj)
      "LocatorLegacyCFI" ->
        this.deserializeLocationLegacyCFI(obj)
      null ->
        this.deserializeLocationLegacyCFI(obj)
      else ->
        throw JSONParseException("Unsupported locator type: $type")
    }
  }

  private fun deserializeLocationLegacyCFI(
    obj: ObjectNode
  ): BookLocation.BookLocationR1 {
    return BookLocation.BookLocationR1(
      progress = JSONParserUtilities.getDoubleDefault(obj, "progressWithinChapter", 0.0),
      contentCFI = JSONParserUtilities.getStringOrNull(obj, "contentCFI"),
      idRef = JSONParserUtilities.getStringOrNull(obj, "idref"),
    )
  }

  private fun deserializeLocationR2(
    obj: ObjectNode
  ): BookLocation.BookLocationR2 {
    val progress =
      BookChapterProgress(
        chapterHref = JSONParserUtilities.getString(obj, "href"),
        chapterProgress = JSONParserUtilities.getDouble(obj, "progressWithinChapter")
      )
    return BookLocation.BookLocationR2(progress)
  }
}
