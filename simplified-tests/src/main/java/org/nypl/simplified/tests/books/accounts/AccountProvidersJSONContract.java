package org.nypl.simplified.tests.books.accounts;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.io7m.jfunctional.Option;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountProvidersJSON;
import org.nypl.simplified.json.core.JSONParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

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

    final AccountProvider p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.id()));
    Assert.assertTrue(c.providers().containsValue(p));
    Assert.assertEquals(Option.some(auth), p.authentication());
    Assert.assertEquals("http://www.librarysimplified.org", p.id().toString());
    Assert.assertEquals("The New York Public Library", p.displayName());
    Assert.assertEquals("Inspiring lifelong learning, advancing knowledge, and strengthening our communities.", p.subtitle());
    Assert.assertEquals(false, p.supportsSimplyESynchronization());
    Assert.assertEquals(false, p.supportsBarcodeDisplay());
    Assert.assertEquals(false, p.supportsBarcodeScanner());
    Assert.assertEquals(true, p.supportsCardCreator());
    Assert.assertEquals(true, p.supportsHelpCenter());
    Assert.assertEquals(true, p.supportsReservations());
    Assert.assertEquals("https://circulation.librarysimplified.org/", p.catalogURI().toString());
    Assert.assertEquals(Option.some("simplyehelp@nypl.org"), p.supportEmail());
    Assert.assertEquals(Option.some(URI.create("http://www.librarysimplified.org/EULA.html")), p.eula());
    Assert.assertEquals(Option.some(URI.create("http://www.librarysimplified.org/license.html")), p.license());
    Assert.assertEquals(Option.some(URI.create("http://www.librarysimplified.org/privacypolicy.html")), p.privacyPolicy());
    Assert.assertEquals("#da2527", p.mainColor());
    Assert.assertEquals(URI.create("LibraryLogoNYPL"), p.logo());
    Assert.assertEquals(Option.some("SimplifiedThemeNoActionBar_NYPL"), p.styleNameOverride());
  }

  @Test
  public final void testSimplyE()
      throws Exception {
    final AccountProviderCollection c =
        AccountProvidersJSON.deserializeFromString(
            readAllFromResource("providers-simplye.json"));

    Assert.assertEquals(1L, c.providers().size());

    final AccountProvider p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.id()));
    Assert.assertTrue(c.providers().containsValue(p));
    Assert.assertEquals("https://instantclassics.librarysimplified.org", p.id().toString());
    Assert.assertEquals("SimplyE Collection", p.displayName());
    Assert.assertEquals("E-books free to download and read without a library card", p.subtitle());
    Assert.assertEquals(false, p.supportsSimplyESynchronization());
    Assert.assertEquals(false, p.supportsBarcodeDisplay());
    Assert.assertEquals(false, p.supportsBarcodeScanner());
    Assert.assertEquals(false, p.supportsCardCreator());
    Assert.assertEquals(false, p.supportsHelpCenter());
    Assert.assertEquals(false, p.supportsReservations());
    Assert.assertEquals("https://instantclassics.librarysimplified.org/index.xml", p.catalogURI().toString());
    Assert.assertEquals(Option.some(URI.create("https://instantclassics.librarysimplified.org/childrens-books.xml")), p.catalogURIForUnder13s());
    Assert.assertEquals(Option.some(URI.create("https://instantclassics.librarysimplified.org/all-books.xml")), p.catalogURIForOver13s());
    Assert.assertEquals(Option.none(), p.supportEmail());
    Assert.assertEquals(Option.none(), p.eula());
    Assert.assertEquals(Option.some(URI.create("http://www.librarysimplified.org/iclicenses.html")), p.license());
    Assert.assertEquals(Option.none(), p.privacyPolicy());
    Assert.assertEquals("#497049", p.mainColor());
    Assert.assertEquals(URI.create("LibraryLogoMagic"), p.logo());
  }

  @Test
  public final void testAll()
      throws Exception {
    final AccountProviderCollection c =
        AccountProvidersJSON.deserializeFromString(
            readAllFromResource("providers-all.json"));

    Assert.assertEquals(10L, c.providers().size());
    final AccountProvider p = c.providerDefault();
    Assert.assertTrue(c.providers().containsKey(p.id()));
    Assert.assertTrue(c.providers().containsValue(p));
  }
}
