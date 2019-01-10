package org.nypl.simplified.books.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.TreeMap;

/**
 * Functions to load account providers from JSON data.
 */

public final class AccountProvidersJSON {

  /**
   * Deserialize an account provider from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider
   * @throws JSONParseException On parse errors
   */

  public static AccountProvider deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj = JSONParserUtilities.checkObject(null, node);
    final AccountProvider.Builder b = AccountProvider.builder();

    b.setId(
      JSONParserUtilities.getURI(obj, "id"));
    b.setCatalogURI(
      JSONParserUtilities.getURI(obj, "catalogUrl"));
    b.setCatalogURIForUnder13s(
      JSONParserUtilities.getURIOptional(obj, "catalogUrlUnder13"));
    b.setCatalogURIForOver13s(
      JSONParserUtilities.getURIOptional(obj, "catalogUrl13"));
    b.setDisplayName(
      JSONParserUtilities.getString(obj, "name"));
    b.setSubtitle(
      JSONParserUtilities.getString(obj, "subtitle"));
    b.setLogo(
      JSONParserUtilities.getURI(obj, "logo"));

    b.setAuthentication(JSONParserUtilities.getObjectOptional(obj, "authentication")
      .mapPartial(sub ->
        AccountProviderAuthenticationDescription.builder()
          .setLoginURI(JSONParserUtilities.getURI(sub, "loginUrl"))
          .setPassCodeLength(JSONParserUtilities.getInteger(sub, "passcodeLength"))
          .setPassCodeMayContainLetters(JSONParserUtilities.getBoolean(sub, "passcodeAllowsLetters"))
          .setRequiresPin(JSONParserUtilities.getBoolean(sub, "requiresPin"))
          .build()));

    b.setSupportsSimplyESynchronization(
      JSONParserUtilities.getBooleanDefault(obj, "supportsSimplyESync", false));
    b.setSupportsBarcodeScanner(
      JSONParserUtilities.getBooleanDefault(obj, "supportsBarcodeScanner", false));
    b.setSupportsBarcodeDisplay(
      JSONParserUtilities.getBooleanDefault(obj, "supportsBarcodeDisplay", false));
    b.setSupportsReservations(
      JSONParserUtilities.getBooleanDefault(obj, "supportsReservations", false));
    b.setSupportsCardCreator(
      JSONParserUtilities.getBooleanDefault(obj, "supportsCardCreator", false));
    b.setSupportsHelpCenter(
      JSONParserUtilities.getBooleanDefault(obj, "supportsHelpCenter", false));

    b.setSupportEmail(
      JSONParserUtilities.getStringOptional(obj, "supportEmail"));
    b.setEula(
      JSONParserUtilities.getURIOptional(obj, "eulaUrl"));
    b.setLicense(
      JSONParserUtilities.getURIOptional(obj, "licenseUrl"));
    b.setPrivacyPolicy(
      JSONParserUtilities.getURIOptional(obj, "privacyUrl"));
    b.setMainColor(
      JSONParserUtilities.getString(obj, "mainColor"));
    b.setStyleNameOverride(
      JSONParserUtilities.getStringOptional(obj, "styleNameOverride"));

    return b.build();
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider collection
   * @throws JSONParseException On parse errors
   */

  public static AccountProviderCollection deserializeFromJSONArray(
    final ObjectMapper jom,
    final ArrayNode node)
    throws JSONParseException {

    final TreeMap<URI, AccountProvider> providers = new TreeMap<>();
    AccountProvider default_provider = null;

    JSONParseException ex = null;
    for (int index = 0; index < node.size(); ++index) {
      try {
        final AccountProvider provider = deserializeFromJSON(jom, node.get(index));
        if (default_provider == null) {
          default_provider = provider;
        }
        if (providers.containsKey(provider.id())) {
          throw new JSONParseException("Duplicate provider ID: " + provider.id());
        }
        providers.put(provider.id(), provider);
      } catch (final JSONParseException e) {
        if (ex == null) {
          ex = e;
        } else {
          ex.addSuppressed(e);
        }
      }
    }

    if (ex != null) {
      throw ex;
    }

    if (providers.isEmpty()) {
      throw new JSONParseException("No providers were parsed.");
    }

    return AccountProviderCollection.create(default_provider, providers);
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param text A JSON string
   * @return A parsed account provider collection
   * @throws IOException On I/O or parser errors
   */

  public static AccountProviderCollection deserializeFromString(
    final String text)
    throws IOException {
    NullCheck.notNull(text, "Text");

    final ObjectMapper jom = new ObjectMapper();
    final JsonNode node = mapNullToTextNode(jom.readTree(text));
    return deserializeFromJSONArray(jom, JSONParserUtilities.checkArray(null, node));
  }

  private static JsonNode mapNullToTextNode(JsonNode jsonNode) {
    if (jsonNode == null) {
      return new TextNode("");
    } else {
      return jsonNode;
    }
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param stream An input stream
   * @return A parsed account provider collection
   * @throws IOException On I/O or parser errors
   */

  public static AccountProviderCollection deserializeFromStream(
    final InputStream stream)
    throws IOException {
    NullCheck.notNull(stream, "Stream");

    final ObjectMapper jom = new ObjectMapper();
    final JsonNode node = mapNullToTextNode(jom.readTree(stream));
    return deserializeFromJSONArray(jom, JSONParserUtilities.checkArray(null, node));
  }
}
