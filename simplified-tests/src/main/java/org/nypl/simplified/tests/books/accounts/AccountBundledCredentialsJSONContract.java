package org.nypl.simplified.tests.books.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jfunctional.Some;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountBarcode;
import org.nypl.simplified.books.accounts.AccountBundledCredentialsJSON;
import org.nypl.simplified.books.accounts.AccountBundledCredentialsType;
import org.nypl.simplified.books.accounts.AccountPIN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

public abstract class AccountBundledCredentialsJSONContract {

  private InputStream resource(
    final String name)
  {
    return AccountBundledCredentialsJSONContract.class.getResourceAsStream(
      "/org/nypl/simplified/tests/books/accounts/" + name);
  }

  @Test
  public void testEmpty()
    throws Exception
  {
    final ObjectMapper mapper = new ObjectMapper();

    try (final InputStream stream = resource("bundled-creds-empty.json")) {
      final AccountBundledCredentialsType credentials =
        AccountBundledCredentialsJSON.deserializeFromStream(mapper, stream);
      Assert.assertEquals(0L, credentials.bundledCredentials().size());
    }
  }

  @Test
  public void testSimple()
    throws Exception
  {
    final ObjectMapper mapper = new ObjectMapper();

    try (final InputStream stream = resource("bundled-creds-simple.json")) {
      final AccountBundledCredentialsType credentials =
        AccountBundledCredentialsJSON.deserializeFromStream(mapper, stream);
      Assert.assertEquals(3L, credentials.bundledCredentials().size());

      final AccountAuthenticationCredentials p0 =
        ((Some<AccountAuthenticationCredentials>) credentials.bundledCredentialsFor(URI.create("urn:0"))).get();
      final AccountAuthenticationCredentials p1 =
        ((Some<AccountAuthenticationCredentials>) credentials.bundledCredentialsFor(URI.create("urn:1"))).get();
      final AccountAuthenticationCredentials p2 =
        ((Some<AccountAuthenticationCredentials>) credentials.bundledCredentialsFor(URI.create("urn:2"))).get();

      Assert.assertEquals(AccountBarcode.create("abcd"), p0.barcode());
      Assert.assertEquals(AccountBarcode.create("efgh"), p1.barcode());
      Assert.assertEquals(AccountBarcode.create("ijkl"), p2.barcode());

      Assert.assertEquals(AccountPIN.create("1234"), p0.pin());
      Assert.assertEquals(AccountPIN.create("5678"), p1.pin());
      Assert.assertEquals(AccountPIN.create("9090"), p2.pin());
    }
  }

  @Test
  public void testSimpleRoundTrip()
    throws Exception
  {
    final ObjectMapper mapper = new ObjectMapper();

    try (final InputStream stream = resource("bundled-creds-simple.json")) {
      final AccountBundledCredentialsType credentials =
        AccountBundledCredentialsJSON.deserializeFromStream(mapper, stream);

      try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        AccountBundledCredentialsJSON.serializeToStream(mapper, credentials, output);

        try (final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
          final AccountBundledCredentialsType alternate =
            AccountBundledCredentialsJSON.deserializeFromStream(mapper, input);

          Assert.assertEquals(credentials, alternate);
        }
      }
    }
  }
}
