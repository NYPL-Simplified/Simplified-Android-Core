package org.nypl.simplified.books.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Functions to serialize and reader book locations to/from JSON.
 */

public final class ReaderBookLocationJSON {

  private ReaderBookLocationJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Deserialize reader book locations from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static ReaderBookLocation deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj =
      JSONParserUtilities.checkObject(null, node);

    return ReaderBookLocation.create(
      JSONParserUtilities.getStringOptional(obj, "contentCFI"),
      JSONParserUtilities.getString(obj, "idref"));
  }

  /**
   * Serialize reader book locations to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
    final ObjectMapper jom,
    final ReaderBookLocation description) {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(description, "Description");

    final ObjectNode jo = jom.createObjectNode();
    description.contentCFI().map_(cfi -> jo.put("contentCFI", cfi));
    jo.put("idref", description.idRef());
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
    final ReaderBookLocation description)
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

  public static ReaderBookLocation deserializeFromString(
    final ObjectMapper jom,
    final String text) throws IOException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(text, "Text");

    final JsonNode node = jom.readTree(text);
    return deserializeFromJSON(jom, JSONParserUtilities.checkObject(null, node));
  }
}
