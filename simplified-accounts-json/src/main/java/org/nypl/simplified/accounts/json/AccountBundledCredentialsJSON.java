package org.nypl.simplified.accounts.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountBarcode;
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType;
import org.nypl.simplified.accounts.api.AccountPIN;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Functions to serialize and deserialize bundled credentials.
 */

public final class AccountBundledCredentialsJSON {

  private AccountBundledCredentialsJSON() {

  }

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param node The JSON
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  public static AccountBundledCredentialsType deserializeFromJSON(
    final ObjectNode node) throws JSONParseException {

    final ConcurrentHashMap<URI, AccountAuthenticationCredentials> credentialsByProvider =
      new ConcurrentHashMap<>();

    final ObjectNode by_provider =
      JSONParserUtilities.getObject(node, "credentialsByProvider");
    final Iterator<String> iter =
      by_provider.fieldNames();

    while (iter.hasNext()) {
      final String name = iter.next();

      final ObjectNode provider_node =
        JSONParserUtilities.getObject(by_provider, name);
      final AccountPIN pin =
        AccountPIN.create(JSONParserUtilities.getString(provider_node, "passWord"));
      final AccountBarcode barcode =
        AccountBarcode.create(JSONParserUtilities.getString(provider_node, "userName"));
      final AccountAuthenticationCredentials credentials =
        AccountAuthenticationCredentials.builder(pin, barcode)
          .build();

      credentialsByProvider.put(URI.create(name), credentials);
    }

    return new BundledCredentials(credentialsByProvider);
  }

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param node The JSON
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  public static AccountBundledCredentialsType deserializeFromJSON(
    final JsonNode node) throws JSONParseException {
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node));
  }

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param mapper The JSON mapper
   * @param stream The input stream
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  public static AccountBundledCredentialsType deserializeFromStream(
    final ObjectMapper mapper,
    final InputStream stream) throws IOException {
    return deserializeFromJSON(mapper.readTree(stream));
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper      The JSON mapper
   * @param credentials The bundled credentials
   * @return The JSON node
   */

  public static ObjectNode serializeToJSON(
    final ObjectMapper mapper,
    final AccountBundledCredentialsType credentials) {
    final ObjectNode node_root = mapper.createObjectNode();
    final ObjectNode node_by_provider = mapper.createObjectNode();

    final Map<URI, AccountAuthenticationCredentials> by_provider =
      credentials.bundledCredentials();

    for (final URI name : by_provider.keySet()) {
      final AccountAuthenticationCredentials creds = by_provider.get(name);
      final ObjectNode node_creds = mapper.createObjectNode();
      node_creds.put("userName", creds.barcode().value());
      node_creds.put("passWord", creds.pin().value());
      node_by_provider.set(name.toString(), node_creds);
    }

    node_root.set("credentialsByProvider", node_by_provider);
    return node_root;
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper      The JSON mapper
   * @param credentials The bundled credentials
   * @return The JSON node
   * @throws JsonProcessingException On serialization errors
   */

  public static byte[] serializeToBytes(
    final ObjectMapper mapper,
    final AccountBundledCredentialsType credentials) throws JsonProcessingException {
    return mapper.writeValueAsBytes(serializeToJSON(mapper, credentials));
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper      The JSON mapper
   * @param credentials The bundled credentials
   * @param stream      The output stream

   * @throws IOException On serialization and I/O errors
   */

  public static void serializeToStream(
    final ObjectMapper mapper,
    final AccountBundledCredentialsType credentials,
    final OutputStream stream) throws IOException {
    mapper.writeValue(stream, serializeToJSON(mapper, credentials));
  }

  private static final class BundledCredentials implements AccountBundledCredentialsType {
    private final Map<URI, AccountAuthenticationCredentials> credentialsByProvider;

    BundledCredentials(
      ConcurrentHashMap<URI, AccountAuthenticationCredentials> credentialsByProvider) {
      this.credentialsByProvider = Collections.unmodifiableMap(credentialsByProvider);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BundledCredentials that = (BundledCredentials) o;
      return this.credentialsByProvider.equals(that.credentialsByProvider);
    }

    @Override
    public int hashCode() {
      return this.credentialsByProvider.hashCode();
    }

    @Override
    public Map<URI, AccountAuthenticationCredentials> bundledCredentials() {
      return this.credentialsByProvider;
    }

    @Override
    public OptionType<AccountAuthenticationCredentials> bundledCredentialsFor(
      final URI accountProvider) {
      return Option.of(this.credentialsByProvider.get(accountProvider));
    }
  }
}
