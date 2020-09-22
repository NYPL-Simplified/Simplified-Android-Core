package org.nypl.simplified.books.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Functions to serialize and reader book locations to/from JSON.
 */

public final class BookLocationJSON {

  private BookLocationJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Deserialize chapter progress from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static BookChapterProgress deserializeProgressFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    Objects.requireNonNull(jom, "Object mapper");
    Objects.requireNonNull(node, "JSON");

    final ObjectNode obj =
      JSONParserUtilities.checkObject(null, node);

    return new BookChapterProgress(
      JSONParserUtilities.getInteger(obj, "chapterIndex"),
      JSONParserUtilities.getDouble(obj, "chapterProgress")
    );
  }

  /**
   * Deserialize reader book locations from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static BookLocation deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    Objects.requireNonNull(jom, "Object mapper");
    Objects.requireNonNull(node, "JSON");

    final ObjectNode obj =
      JSONParserUtilities.checkObject(null, node);
    final ObjectNode progressNode =
      JSONParserUtilities.getObjectOrNull(obj, "progress");

    final BookChapterProgress progress;
    if (progressNode != null) {
      progress = deserializeProgressFromJSON(jom, progressNode);
    } else {
      progress = null;
    }

    return new BookLocation(
      progress,
      JSONParserUtilities.getStringOrNull(obj, "contentCFI"),
      JSONParserUtilities.getStringOrNull(obj, "idref")
    );
  }

  /**
   * Serialize reader book locations to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
    final ObjectMapper jom,
    final BookLocation description) {

    Objects.requireNonNull(jom, "Object mapper");
    Objects.requireNonNull(description, "Description");

    final ObjectNode jo = jom.createObjectNode();
    final String contentCFI = description.getContentCFI();
    if (contentCFI != null) {
      jo.put("contentCFI", contentCFI);
    }
    final String idRef = description.getIdRef();
    if (idRef != null) {
      jo.put("idref", idRef);
    }

    final BookChapterProgress progress = description.getProgress();
    if (progress != null) {
      final ObjectNode progressNode = jom.createObjectNode();
      progressNode.put("chapterIndex", progress.getChapterIndex());
      progressNode.put("chapterProgress", progress.getChapterProgress());
      jo.set("progress", progressNode);
    }
    return jo;
  }

  /**
   * Serialize reader book locations to a JSON string.
   *
   * @param jom A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  public static String serializeToString(
    final ObjectMapper jom,
    final BookLocation description)
    throws IOException {

    final ObjectNode jo = serializeToJSON(jom, description);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }

  /**
   * Deserialize a reader book location from the given string.
   *
   * @param jom A JSON object mapper
   * @return A parsed location
   * @throws IOException On I/O or parser errors
   */

  public static BookLocation deserializeFromString(
    final ObjectMapper jom,
    final String text) throws IOException {

    Objects.requireNonNull(jom, "Object mapper");
    Objects.requireNonNull(text, "Text");

    final JsonNode node = jom.readTree(text);
    return deserializeFromJSON(jom, JSONParserUtilities.checkObject(null, node));
  }
}
