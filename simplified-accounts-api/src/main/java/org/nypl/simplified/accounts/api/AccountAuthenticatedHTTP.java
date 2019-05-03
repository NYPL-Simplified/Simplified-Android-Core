package org.nypl.simplified.accounts.api;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPOAuthToken;

/**
 * Convenient functions to construct authenticated HTTP instances from sets of credentials.
 */

public final class AccountAuthenticatedHTTP {

  private AccountAuthenticatedHTTP() {
    throw new UnreachableCodeException();
  }

  /**
   * Create an authenticated HTTP instance from the given credentials.
   *
   * @param creds The credentials
   * @return An HTTP instance
   */

  public static HTTPAuthType createAuthenticatedHTTP(final AccountAuthenticationCredentials creds) {
    NullCheck.notNull(creds, "Credentials");

    return creds.oAuthToken().accept(new OptionVisitorType<HTTPOAuthToken, HTTPAuthType>() {
      @Override
      public HTTPAuthType none(None<HTTPOAuthToken> none) {
        return HTTPAuthBasic.create(creds.barcode().value(), creds.pin().value());
      }

      @Override
      public HTTPAuthType some(Some<HTTPOAuthToken> some) {
        return HTTPAuthOAuth.create(some.get());
      }
    });
  }
}
