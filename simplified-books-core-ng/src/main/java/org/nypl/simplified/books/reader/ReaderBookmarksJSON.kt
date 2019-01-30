package org.nypl.simplified.books.reader

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.ImmutableMap
import com.io7m.jnull.NullCheck
import com.io7m.junreachable.UnreachableCodeException

import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.HashMap

/**
 * Functions to serialize and reader bookmarks to/from JSON.
 *
 * @deprecated Use ReaderBookmarkJSON
 */

@Deprecated(message = "Use ReaderBookmarkJSON")
class ReaderBookmarksJSON private constructor() {

  init {
    throw UnreachableCodeException()
  }

  companion object {

    /**
     * Deserialize profile preferences from the given JSON node.
     *
     * @param jom  A JSON object mapper
     * @param node A JSON node
     * @return A parsed description
     * @throws JSONParseException On parse errors
     */

    @JvmStatic
    @Throws(JSONParseException::class)
    fun deserializeFromJSON(
      jom: ObjectMapper,
      node: JsonNode): ReaderBookmarks {

      NullCheck.notNull(jom, "Object mapper")
      NullCheck.notNull(node, "JSON")

      val obj = JSONParserUtilities.checkObject(null, node)

      val bookmarks_builder = HashMap<BookID, ReaderBookLocation>()
      val by_id = JSONParserUtilities.getObject(obj, "locations-by-book-id")

      val field_iter = by_id.fieldNames()
      while (field_iter.hasNext()) {
        val field = field_iter.next()
        val book_id = BookID.create(field)
        val location = ReaderBookLocationJSON.deserializeFromJSON(jom, JSONParserUtilities.getNode(by_id, field))
        bookmarks_builder[book_id] = location
      }

      return ReaderBookmarks.create(ImmutableMap.copyOf(bookmarks_builder))
    }

    /**
     * Serialize profile preferences to JSON.
     *
     * @param jom A JSON object mapper
     * @return A serialized object
     */

    @JvmStatic
    fun serializeToJSON(
      jom: ObjectMapper,
      description: ReaderBookmarks): ObjectNode {

      val jo = jom.createObjectNode()
      val byID = jom.createObjectNode()
      for ((bookID, location) in description.bookmarks()) {
        byID.set(bookID.value(), ReaderBookLocationJSON.serializeToJSON(jom, location))
      }

      jo.set("locations-by-book-id", byID)
      return jo
    }

    /**
     * Serialize profile preferences to a JSON string.
     *
     * @param jom A JSON object mapper
     * @return A JSON string
     * @throws IOException On serialization errors
     */

    @JvmStatic
    @Throws(IOException::class)
    fun serializeToString(
      jom: ObjectMapper,
      description: ReaderBookmarks): String {

      val jo = serializeToJSON(jom, description)
      val bao = ByteArrayOutputStream(1024)
      JSONSerializerUtilities.serialize(jo, bao)
      return bao.toString("UTF-8")
    }
  }
}
