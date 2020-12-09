package org.nypl.simplified.tests.books.accounts;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

public abstract class AccountProvidersJSONContract {

  @Rule public final ExpectedException expected = ExpectedException.none();

  private static InputStream readAllFromResource(
      final String name)
      throws Exception {

    final URL url =
      AccountProvidersJSONContract.class.getResource(
        "/org/nypl/simplified/tests/books/accounts/" + name);
    return url.openStream();
  }

  @Test
  public final void testEmpty() throws Exception {
    expected.expect(JSONParseException.class);
    deserializeFromString("");
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
    expected.expect(JSONParseException.class);
    deserializeFromString("{}");
  }

  @Test
  public final void testDuplicateProvider()
      throws Exception {
    expected.expect(JSONParseException.class);
    expected.expectMessage(StringContains.containsString("Duplicate provider"));
    AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-duplicate.json"));
  }

  @Test
  public final void testMultipleAuthenticationTypes0()
    throws Exception {
    Map<URI, AccountProvider> providers =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
      readAllFromResource("providers-multi-auth-0.json"));

    Assert.assertEquals(1, providers.size());

    final AccountProvider provider =
      providers.get(URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"));

    Assert.assertEquals(AccountProviderAuthenticationDescription.Basic.class, provider.getAuthentication().getClass());
    Assert.assertEquals(0, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
      AccountProvidersJSON.INSTANCE.deserializeFromJSON(
        AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assert.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testMultipleAuthenticationTypes1()
    throws Exception {
    Map<URI, AccountProvider> providers =
      AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
        readAllFromResource("providers-multi-auth-1.json"));

    Assert.assertEquals(1, providers.size());

    final AccountProvider provider =
      providers.get(URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"));

    Assert.assertEquals(AccountProviderAuthenticationDescription.Basic.class, provider.getAuthentication().getClass());
    Assert.assertEquals(2, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
      AccountProvidersJSON.INSTANCE.deserializeFromJSON(
        AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assert.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testSAML2()
          throws Exception {
    Map<URI, AccountProvider> providers =
            AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
                    readAllFromResource("providers-saml.json"));

    Assert.assertEquals(1, providers.size());

    final AccountProvider provider =
            providers.get(URI.create("urn:uuid:b67588ef-d3ce-4187-9709-04e6f4c01a13"));

    Assert.assertEquals(AccountProviderAuthenticationDescription.SAML2_0.class, provider.getAuthentication().getClass());
    Assert.assertEquals(0, provider.getAuthenticationAlternatives().size());

    final AccountProvider providerAfter =
            AccountProvidersJSON.INSTANCE.deserializeFromJSON(
                    AccountProvidersJSON.INSTANCE.serializeToJSON(provider));

    Assert.assertEquals(providerAfter, provider);
  }

  @Test
  public final void testAll()
      throws Exception {
    final Map<URI, AccountProvider> c =
        AccountProvidersJSON.INSTANCE.deserializeCollectionFromStream(
            readAllFromResource("providers-all.json"));

    Assert.assertEquals(172L, c.size());
  }
}
