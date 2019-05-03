package org.nypl.simplified.tests.books.accounts;

import com.io7m.jfunctional.Option;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationProvider;
import org.nypl.simplified.accounts.api.AccountBarcode;
import org.nypl.simplified.accounts.api.AccountPIN;
import org.nypl.simplified.accounts.api.AccountPatron;
import org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsJSON;
import org.nypl.simplified.http.core.HTTPOAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @see AccountAuthenticationCredentialsJSON
 */

public abstract class AccountAuthenticationCredentialsJSONContract {

  private static final Logger LOG =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsJSONContract.class);

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public final void testRoundTrip0()
      throws Exception {

    final AccountAuthenticationCredentials creds0 =
        AccountAuthenticationCredentials.builder(
            AccountPIN.create("1234"), AccountBarcode.create("5678"))
            .build();

    final AccountAuthenticationCredentials creds1 =
        AccountAuthenticationCredentialsJSON.deserializeFromJSON(
            AccountAuthenticationCredentialsJSON.serializeToJSON(creds0));

    Assert.assertEquals(creds0, creds1);
  }

  @Test
  public final void testRoundTrip1()
      throws Exception {

    final AccountAuthenticationCredentials creds0 =
        AccountAuthenticationCredentials.builder(
            AccountPIN.create("1234"), AccountBarcode.create("5678"))
            .setPatron(Option.some(AccountPatron.create("patron")))
            .setAuthenticationProvider(Option.some(AccountAuthenticationProvider.create("provider")))
            .setOAuthToken(Option.some(HTTPOAuthToken.create("oauth")))
            .build();

    final AccountAuthenticationCredentials creds1 =
        AccountAuthenticationCredentialsJSON.deserializeFromJSON(
            AccountAuthenticationCredentialsJSON.serializeToJSON(creds0));

    Assert.assertEquals(creds0, creds1);
  }

  @Test
  public final void testRoundTrip2()
      throws Exception {

    AccountAuthenticationAdobePreActivationCredentials adobe =
        AccountAuthenticationAdobePreActivationCredentials.create(
            new AdobeVendorID("vendor"),
            AccountAuthenticationAdobeClientToken.create("NYNYPL|156|5e0cdf28-e3a2-11e7-ab18-0e26ed4612aa|LEcBeSV"),
            URI.create("http://example.com"),
            Option.<AccountAuthenticationAdobePostActivationCredentials>none());

    final AccountAuthenticationCredentials creds0 =
        AccountAuthenticationCredentials.builder(
            AccountPIN.create("1234"), AccountBarcode.create("5678"))
            .setPatron(Option.some(AccountPatron.create("patron")))
            .setAuthenticationProvider(Option.some(AccountAuthenticationProvider.create("provider")))
            .setOAuthToken(Option.some(HTTPOAuthToken.create("oauth")))
            .setAdobeCredentials(Option.some(adobe))
            .build();

    final AccountAuthenticationCredentials creds1 =
        AccountAuthenticationCredentialsJSON.deserializeFromJSON(
            AccountAuthenticationCredentialsJSON.serializeToJSON(creds0));

    Assert.assertEquals(creds0, creds1);
  }

  @Test
  public final void testRoundTrip3()
      throws Exception {

    AccountAuthenticationAdobePostActivationCredentials adobe_post =
        AccountAuthenticationAdobePostActivationCredentials.create(
            new AdobeDeviceID("device"),
            new AdobeUserID("user"));

    AccountAuthenticationAdobePreActivationCredentials adobe =
        AccountAuthenticationAdobePreActivationCredentials.create(
            new AdobeVendorID("vendor"),
            AccountAuthenticationAdobeClientToken.create("NYNYPL|156|5e0cdf28-e3a2-11e7-ab18-0e26ed4612aa|LEcBeSV"),
            URI.create("http://example.com"),
            Option.some(adobe_post));

    final AccountAuthenticationCredentials creds0 =
        AccountAuthenticationCredentials.builder(
            AccountPIN.create("1234"), AccountBarcode.create("5678"))
            .setPatron(Option.some(AccountPatron.create("patron")))
            .setAuthenticationProvider(Option.some(AccountAuthenticationProvider.create("provider")))
            .setOAuthToken(Option.some(HTTPOAuthToken.create("oauth")))
            .setAdobeCredentials(Option.some(adobe))
            .build();

    final AccountAuthenticationCredentials creds1 =
        AccountAuthenticationCredentialsJSON.deserializeFromJSON(
            AccountAuthenticationCredentialsJSON.serializeToJSON(creds0));

    Assert.assertEquals(creds0, creds1);
  }
}
