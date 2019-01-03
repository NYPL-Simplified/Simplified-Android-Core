package org.nypl.simplified.books.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.nypl.simplified.books.reader.ReaderColorScheme.SCHEME_BLACK_ON_WHITE;
import static org.nypl.simplified.books.reader.ReaderFontSelection.READER_FONT_SANS_SERIF;

/**
 * Functions to serialize and reader bookmarks to/from JSON.
 */

public final class ReaderBookmarksJSON {

  private ReaderBookmarksJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Deserialize profile preferences from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static ReaderBookmarks deserializeFromJSON(
      final ObjectMapper jom,
      final JsonNode node)
      throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj =
        JSONParserUtilities.checkObject(null, node);

    final HashMap<BookID, ReaderBookLocation> bookmarks_builder = new HashMap<>();
    final ObjectNode by_id = JSONParserUtilities.getObject(obj, "locations-by-book-id");

    final Iterator<String> field_iter = by_id.fieldNames();
    while (field_iter.hasNext()) {
      final String field =
          field_iter.next();
      final BookID book_id =
          BookID.create(field);
      final ReaderBookLocation location =
          ReaderBookLocationJSON.deserializeFromJSON(jom, JSONParserUtilities.getNode(by_id, field));
      bookmarks_builder.put(book_id, location);
    }

    return ReaderBookmarks.create(ImmutableMap.copyOf(bookmarks_builder));
  }

  /**
   * Serialize profile preferences to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
      final ObjectMapper jom,
      final ReaderBookmarks description) {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(description, "Description");

    final ObjectNode jo = jom.createObjectNode();
    final ObjectNode by_id = jom.createObjectNode();
    for (final Map.Entry<BookID, ReaderBookLocation> e : description.bookmarks().entrySet()) {
      final BookID book_id = e.getKey();
      final ReaderBookLocation location = e.getValue();
      by_id.set(book_id.value(), ReaderBookLocationJSON.serializeToJSON(jom, location));
    }

    jo.set("locations-by-book-id", by_id);
    return jo;
  }

  /**
   * Serialize profile preferences to a JSON string.
   *
   * @param jom A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  public static String serializeToString(
      final ObjectMapper jom,
      final ReaderBookmarks description)
      throws IOException {

    final ObjectNode jo = serializeToJSON(jom, description);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }
}
