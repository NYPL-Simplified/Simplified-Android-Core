package org.nypl.simplified.books.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.profiles.ProfileDescription;
import org.nypl.simplified.books.profiles.ProfilePreferences;
import org.nypl.simplified.books.profiles.ProfilePreferencesJSON;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.nypl.simplified.books.reader.ReaderColorScheme.*;
import static org.nypl.simplified.books.reader.ReaderFontSelection.*;

/**
 * Functions to serialize and reader preferences to/from JSON.
 */

public final class ReaderPreferencesJSON {

  private ReaderPreferencesJSON() {
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

  public static ReaderPreferences deserializeFromJSON(
      final ObjectMapper jom,
      final JsonNode node)
      throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj =
        JSONParserUtilities.checkObject(null, node);

    final double font_scale =
        JSONParserUtilities.getDoubleDefault(
            obj, "font_scale", 100.0);
    final ReaderFontSelection font_family =
        fontFamily(JSONParserUtilities.getStringDefault(
            obj, "font_family", READER_FONT_SANS_SERIF.name()));
    final ReaderColorScheme color_scheme =
        colorScheme(JSONParserUtilities.getStringDefault(
            obj, "color_scheme", SCHEME_BLACK_ON_WHITE.name()));

    return ReaderPreferences.builder()
        .setFontScale(font_scale)
        .setFontFamily(font_family)
        .setColorScheme(color_scheme)
        .build();
  }

  private static ReaderColorScheme colorScheme(final String font_family) throws JSONParseException {
    try {
      return ReaderColorScheme.valueOf(font_family);
    } catch (final IllegalArgumentException e) {
      throw new JSONParseException("Unparseable color scheme", e);
    }
  }

  private static ReaderFontSelection fontFamily(final String font_family) throws JSONParseException {
    try {
      return ReaderFontSelection.valueOf(font_family);
    } catch (final IllegalArgumentException e) {
      throw new JSONParseException("Unparseable font family", e);
    }
  }

  /**
   * Serialize profile preferences to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
      final ObjectMapper jom,
      final ReaderPreferences description) {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(description, "Description");

    final ObjectNode jo = jom.createObjectNode();
    jo.put("font_scale", description.fontScale());
    jo.put("font_family", description.fontFamily().name());
    jo.put("color_scheme", description.colorScheme().name());
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
      final ReaderPreferences description)
      throws IOException {

    final ObjectNode jo = serializeToJSON(jom, description);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }
}
