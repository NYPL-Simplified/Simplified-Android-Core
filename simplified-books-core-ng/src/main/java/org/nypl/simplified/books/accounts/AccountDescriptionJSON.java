package org.nypl.simplified.books.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Functions to serialize and deserialize account descriptions to/from JSON.
 */

public final class AccountDescriptionJSON {

  private AccountDescriptionJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Deserialize a account description from the given file.
   *
   * @param jom  A JSON object mapper
   * @param file A file
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  public static AccountDescription deserializeFromFile(
    final ObjectMapper jom,
    final File file)
    throws IOException {
    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(file, "File");
    return deserializeFromText(jom, FileUtilities.fileReadUTF8(file));
  }

  /**
   * Deserialize a account description from the given text.
   *
   * @param jom  A JSON object mapper
   * @param text A JSON string
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  public static AccountDescription deserializeFromText(
    final ObjectMapper jom,
    final String text) throws IOException {
    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(text, "Text");
    return deserializeFromJSON(jom, jom.readTree(text));
  }

  /**
   * Deserialize a account description from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static AccountDescription deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {
    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj =
      JSONParserUtilities.checkObject(null, node);

    final AccountPreferences preferences;
    if (obj.has("preferences")) {
      preferences = AccountPreferencesJSON.INSTANCE.deserializeFromJSON(
        JSONParserUtilities.getObject(obj, "preferences"));
    } else {
      preferences = AccountPreferences.Companion.defaultPreferences();
    }

    final AccountDescription.Builder builder =
      AccountDescription.builder(
        JSONParserUtilities.getURI(obj, "provider"),
        preferences);

    builder.setCredentials(
      JSONParserUtilities.getObjectOptional(obj, "credentials")
        .mapPartial(AccountAuthenticationCredentialsJSON::deserializeFromJSON));

    return builder.build();
  }

  /**
   * Serialize a account description to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
    final ObjectMapper jom,
    final AccountDescription description) {
    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(description, "Description");

    final ObjectNode jo = jom.createObjectNode();
    jo.put("provider", description.provider().toString());

    description.credentials().map_(
      creds -> jo.set("credentials", AccountAuthenticationCredentialsJSON.serializeToJSON(creds)));
    return jo;
  }

  /**
   * Serialize a account description to a JSON string.
   *
   * @param jom A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  public static String serializeToString(
    final ObjectMapper jom,
    final AccountDescription description)
    throws IOException {
    final ObjectNode jo = serializeToJSON(jom, description);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }
}
