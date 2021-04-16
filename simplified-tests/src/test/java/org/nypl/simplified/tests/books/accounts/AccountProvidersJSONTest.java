package org.nypl.simplified.tests.books.accounts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.accounts.api.AccountProvider;
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription;
import org.nypl.simplified.accounts.json.AccountProvidersJSON;
import org.nypl.simplified.json.core.JSONParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public final class AccountProvidersJSONTest {

  private static InputStream readAllFromResource(
    final String name)
    throws Exception {

    final URL url =
      AccountProvidersJSONTest.class.getResource(
        "/org/nypl/simplified/tests/books/accounts/" + name);
    return url.openStream();
  }

  @Test
  public final void testEmpty() throws Exception {
    Assertions.assertThrows(JSONParseException.class, () -> {
      deserializeFromString("");
    });
  }

  private void deserializeFromString(String text) throws IOException {
    AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
      new ByteArrayInputStream(text.getBytes()));
  }

  @Test
  public final void testEmptyArray() throws Exception {
    deserializeFromString("[]");
  }

  @Test
  public final void testEmptyWrongType()
    throws Exception {
    Assertions.assertThrows(JSONParseException.class, () -> {
      deserializeFromString("{}");
    });
  }

  @Test
  public final void testDuplicateProvider()
    throws Exception {

    JSONParseException ex = Assertions.assertThrows(JSONParseException.class, () -> {
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-duplicate.json"));
    });
    Assertions.assertTrue(ex.getMessage().contains("Duplicate provider"));
  }

  @Test
  public final void testMultipleAuthenticationTypes0()
    throws Exception {
    Map<URI, AccountProvider> providers =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-multi-auth-0.json"));

    Assertions.assertEquals(1, providers.size());

    final AccountProvider provider =
      providers.get(URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"));

    Assertions.assertEquals(AccountProviderAuthenticationDescription.Basic.class, provider.getAuthentication().getClass());
    Assertions.assertEquals(0, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
      AccountProvidersJSON.INSTANCE.deserializeFromJSON(
        AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assertions.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testMultipleAuthenticationTypes1()
    throws Exception {
    Map<URI, AccountProvider> providers =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-multi-auth-1.json"));

    Assertions.assertEquals(1, providers.size());

    final AccountProvider provider =
      providers.get(URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"));

    Assertions.assertEquals(AccountProviderAuthenticationDescription.Basic.class, provider.getAuthentication().getClass());
    Assertions.assertEquals(2, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
      AccountProvidersJSON.INSTANCE.deserializeFromJSON(
        AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assertions.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testSAML2()
    throws Exception {
    Map<URI, AccountProvider> providers =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-saml.json"));

    Assertions.assertEquals(1, providers.size());

    final AccountProvider provider =
      providers.get(URI.create("urn:uuid:b67588ef-d3ce-4187-9709-04e6f4c01a13"));

    Assertions.assertEquals(AccountProviderAuthenticationDescription.SAML2_0.class, provider.getAuthentication().getClass());
    Assertions.assertEquals(0, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
      AccountProvidersJSON.INSTANCE.deserializeFromJSON(
        AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assertions.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testAll()
    throws Exception {
    final Map<URI, AccountProvider> c =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-all.json"));

    Assertions.assertEquals(172L, c.size());
  }
}
