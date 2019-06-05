package org.nypl.simplified.tests.books.accounts;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.database.AccountProviderCollection;
import org.nypl.simplified.accounts.database.AccountProvidersJSON;
import org.nypl.simplified.json.core.JSONParseException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public abstract class AccountProvidersJSONContract {

  @Rule public final ExpectedException expected = ExpectedException.none();

  private static String readAllFromResource(
      final String name)
      throws Exception {

    final URL url = AccountProvidersJSONContract.class.getResource(
        "/org/nypl/simplified/tests/books/accounts/" + name);

    final byte[] buffer = new byte[1024];
    try (InputStream stream = url.openStream()) {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        while (true) {
          final int r = stream.read(buffer);
          if (r <= 0) {
            break;
          }
          out.write(buffer, 0, r);
        }
        return out.toString("UTF-8");
      }
    }
  }

  @Test
  public final void testEmpty()
      throws Exception {
    expected.expect(JSONParseException.class);
    AccountProvidersJSON.deserializeFromString("");
  }

  @Test
  public final void testEmptyArray()
      throws Exception {
    expected.expect(JSONParseException.class);
    expected.expectMessage(StringContains.containsString("No providers were parsed"));
    AccountProvidersJSON.deserializeFromString("[]");
  }

  @Test
  public final void testEmptyWrongType()
      throws Exception {
    expected.expect(JSONParseException.class);
    AccountProvidersJSON.deserializeFromString("{}");
  }

  @Test
  public final void testDuplicateProvider()
      throws Exception {
    expected.expect(JSONParseException.class);
    expected.expectMessage(StringContains.containsString("Duplicate provider"));
    AccountProvidersJSON.deserializeFromString(
        readAllFromResource("providers-duplicate.json"));
  }

  @Test
  public final void testNYPL()
      throws Exception {
    final AccountProviderCollection c =
        AccountProvidersJSON.deserializeFromString(
            readAllFromResource("providers-nypl.json"));

    Assert.assertEquals(1L, c.providers().size());

    final AccountProviderAuthenticationDescription auth =
        AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("https://circulation.librarysimplified.org/loans/"))
        .setPassCodeMayContainLetters(false)
        .setPassCodeLength(4)
        .setRequiresPin(true)
        .build();

    final AccountProviderType p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.getId()));
    Assert.assertTrue(c.providers().containsValue(p));
    Assert.assertEquals(auth, p.getAuthentication());
    Assert.assertEquals("http://www.librarysimplified.org", p.getId().toString());
    Assert.assertEquals("The New York Public Library", p.getDisplayName());
    Assert.assertEquals("Inspiring lifelong learning, advancing knowledge, and strengthening our communities.", p.getSubtitle());
    Assert.assertEquals(false, p.getSupportsSimplyESynchronization());
    Assert.assertEquals(false, p.getSupportsBarcodeDisplay());
    Assert.assertEquals(false, p.getSupportsBarcodeScanner());
    Assert.assertEquals(true, p.getSupportsCardCreator());
    Assert.assertEquals(true, p.getSupportsHelpCenter());
    Assert.assertEquals(true, p.getSupportsReservations());
    Assert.assertEquals("https://circulation.librarysimplified.org/", p.getCatalogURI().toString());
    Assert.assertEquals("simplyehelp@nypl.org", p.getSupportEmail());
    Assert.assertEquals(URI.create("http://www.librarysimplified.org/EULA.html"), p.getEula());
    Assert.assertEquals(URI.create("http://www.librarysimplified.org/license.html"), p.getLicense());
    Assert.assertEquals(URI.create("http://www.librarysimplified.org/privacypolicy.html"), p.getPrivacyPolicy());
    Assert.assertEquals("#da2527", p.getMainColor());
    Assert.assertEquals(URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo="), p.getLogo());
    Assert.assertEquals("SimplifiedThemeNoActionBar_NYPL", p.getStyleNameOverride());
  }

  @Test
  public final void testSimplyE()
      throws Exception {
    final AccountProviderCollection c =
        AccountProvidersJSON.deserializeFromString(
            readAllFromResource("providers-simplye.json"));

    Assert.assertEquals(1L, c.providers().size());

    final AccountProviderType p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.getId()));
    Assert.assertTrue(c.providers().containsValue(p));
    Assert.assertEquals("https://circulation.librarysimplified.org/CLASSICS/authentication_document", p.getAuthenticationDocumentURI().toString());
    Assert.assertEquals("https://instantclassics.librarysimplified.org", p.getId().toString());
    Assert.assertEquals("SimplyE Collection", p.getDisplayName());
    Assert.assertEquals("E-books free to download and read without a library card", p.getSubtitle());
    Assert.assertEquals(false, p.getSupportsSimplyESynchronization());
    Assert.assertEquals(false, p.getSupportsBarcodeDisplay());
    Assert.assertEquals(false, p.getSupportsBarcodeScanner());
    Assert.assertEquals(false, p.getSupportsCardCreator());
    Assert.assertEquals(false, p.getSupportsHelpCenter());
    Assert.assertEquals(false, p.getSupportsReservations());
    Assert.assertEquals("https://instantclassics.librarysimplified.org/index.xml", p.getCatalogURI().toString());
    Assert.assertEquals(URI.create("https://instantclassics.librarysimplified.org/childrens-books.xml"), p.getCatalogURIForUnder13s());
    Assert.assertEquals(URI.create("https://instantclassics.librarysimplified.org/all-books.xml"), p.getCatalogURIForOver13s());
    Assert.assertEquals(null, p.getSupportEmail());
    Assert.assertEquals(null, p.getEula());
    Assert.assertEquals(URI.create("http://www.librarysimplified.org/iclicenses.html"), p.getLicense());
    Assert.assertEquals(null, p.getPrivacyPolicy());
    Assert.assertEquals("#497049", p.getMainColor());
    Assert.assertEquals(URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo="), p.getLogo());
  }

  @Test
  public final void testAll()
      throws Exception {
    final AccountProviderCollection c =
        AccountProvidersJSON.deserializeFromString(
            readAllFromResource("providers-all.json"));

    Assert.assertEquals(172L, c.providers().size());
    final AccountProviderType p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.getId()));
    Assert.assertTrue(c.providers().containsValue(p));
  }
}
