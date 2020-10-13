package org.nypl.simplified.tests.books.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType;
import org.nypl.simplified.accounts.api.AccountPassword;
import org.nypl.simplified.accounts.api.AccountUsername;
import org.nypl.simplified.accounts.json.AccountBundledCredentialsJSON;

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

      final AccountAuthenticationCredentials.Basic p0 =
        (AccountAuthenticationCredentials.Basic) credentials.bundledCredentialsFor(URI.create("urn:0"));
      final AccountAuthenticationCredentials.Basic p1 =
        (AccountAuthenticationCredentials.Basic) credentials.bundledCredentialsFor(URI.create("urn:1"));
      final AccountAuthenticationCredentials.Basic p2 =
        (AccountAuthenticationCredentials.Basic) credentials.bundledCredentialsFor(URI.create("urn:2"));

      Assert.assertEquals(new AccountUsername("abcd"), p0.getUserName());
      Assert.assertEquals(new AccountUsername("efgh"), p1.getUserName());
      Assert.assertEquals(new AccountUsername("ijkl"), p2.getUserName());

      Assert.assertEquals(new AccountPassword("1234"), p0.getPassword());
      Assert.assertEquals(new AccountPassword("5678"), p1.getPassword());
      Assert.assertEquals(new AccountPassword("9090"), p2.getPassword());
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
