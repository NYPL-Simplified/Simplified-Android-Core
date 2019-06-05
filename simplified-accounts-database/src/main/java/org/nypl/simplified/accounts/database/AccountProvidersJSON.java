package org.nypl.simplified.accounts.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription;
import org.nypl.simplified.accounts.api.AccountProviderBuilderType;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.api.AccountProviders;
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

  public static AccountProviderType deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final ObjectNode obj = JSONParserUtilities.checkObject(null, node);
    final AccountProviderBuilderType b = AccountProviders.builder();

    final URI id_uuid = JSONParserUtilities.getURI(obj, "id_uuid");

    try {
      b.setId(id_uuid);

      final URI catalogUrl = JSONParserUtilities.getURI(obj, "catalogUrl");
      b.setCatalogURI(catalogUrl);
      b.setPatronSettingsURI(applyPatronSettingHack(catalogUrl));
      b.setAnnotationsURI(applyAnnotationsHack(catalogUrl));

      b.setAuthenticationDocumentURI(
        JSONParserUtilities.getURIOrNull(obj, "authenticationDocument"));
      b.setCatalogURIForUnder13s(
        JSONParserUtilities.getURIOrNull(obj, "catalogUrlUnder13"));
      b.setCatalogURIForOver13s(
        JSONParserUtilities.getURIOrNull(obj, "catalogUrl13"));
      b.setDisplayName(
        JSONParserUtilities.getString(obj, "name"));
      b.setSubtitle(
        JSONParserUtilities.getStringOrNull(obj, "subtitle"));
      b.setLogo(
        JSONParserUtilities.getURIOrNull(obj, "logo"));

      if (JSONParserUtilities.getBooleanDefault(obj, "needsAuth", false)) {
        final AccountProviderAuthenticationDescription.Builder authentication_builder =
          AccountProviderAuthenticationDescription.builder();

        authentication_builder.setRequiresPin(
          JSONParserUtilities.getBooleanDefault(obj, "pinRequired", true));
        authentication_builder.setPassCodeLength(
          JSONParserUtilities.getIntegerDefault(obj, "authPasscodeLength", 0));
        authentication_builder.setPassCodeMayContainLetters(
          JSONParserUtilities.getBooleanDefault(obj, "authPasscodeAllowsLetters", true));
        authentication_builder.setLoginURI(
          JSONParserUtilities.getURIDefault(obj, "loginUrl", applyLoansHack(catalogUrl)));

        b.setAuthentication(authentication_builder.build());
      } else {
        b.setAuthentication(null);
      }

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
        JSONParserUtilities.getStringOrNull(obj, "supportEmail"));
      b.setEula(
        JSONParserUtilities.getURIOrNull(obj, "eulaUrl"));
      b.setLicense(
        JSONParserUtilities.getURIOrNull(obj, "licenseUrl"));
      b.setPrivacyPolicy(
        JSONParserUtilities.getURIOrNull(obj, "privacyUrl"));
      b.setMainColor(
        JSONParserUtilities.getString(obj, "mainColor"));
      b.setStyleNameOverride(
        JSONParserUtilities.getStringOrNull(obj, "styleNameOverride"));

      return b.build();
    } catch (JSONParseException e) {
      throw new JSONParseException("Unable to parse provider " + id_uuid, e);
    }
  }

  private static URI applyAnnotationsHack(URI catalogUrl) {
    final String text = catalogUrl.toString().replaceAll("/+$", "");
    return URI.create(text + "/annotations/");
  }

  private static URI applyPatronSettingHack(URI catalogUrl) {
    final String text = catalogUrl.toString().replaceAll("/+$", "");
    return URI.create(text + "/patrons/me/");
  }

  private static URI applyLoansHack(URI catalogUrl) {
    final String text = catalogUrl.toString().replaceAll("/+$", "");
    return URI.create(text + "/loans/");
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

    final TreeMap<URI, AccountProviderType> providers = new TreeMap<>();
    AccountProviderType default_provider = null;

    JSONParseException ex = null;
    for (int index = 0; index < node.size(); ++index) {
      try {
        final AccountProviderType provider = deserializeFromJSON(jom, node.get(index));
        if (default_provider == null) {
          default_provider = provider;
        }
        if (providers.containsKey(provider.getId())) {
          throw new JSONParseException("Duplicate provider ID: " + provider.getId());
        }
        providers.put(provider.getId(), provider);
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
