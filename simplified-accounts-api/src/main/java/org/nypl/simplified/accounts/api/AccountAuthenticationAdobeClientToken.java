package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;
import com.io7m.jnull.NullCheck;

/**
 * <p>A token used by Adobe DRM to activate and deactivate devices.</p>
 * <p>This is received by clients in OPDS feeds as a pipe ("|") separated string similar to {@code "NYNYPL|1513878186|5e3cdf28-e3a2-11e7-ab18-0e26ed4612aa|LEcBeSVavfkJRIRd5cRWdUK5p7DZjuoxwwKpoPIqKLA@"}</p>
 */

@AutoValue
public abstract class AccountAuthenticationAdobeClientToken {

  AccountAuthenticationAdobeClientToken() {

  }

  /**
   * Construct a token.
   *
   * @param in_token The raw token value
   */

  public static AccountAuthenticationAdobeClientToken create(
      final String in_token) {

    NullCheck.notNull(in_token, "Token");

    final String username = in_token.substring(0, in_token.lastIndexOf("|"));
    final String password = in_token.substring(in_token.lastIndexOf("|") + 1);
    return new AutoValue_AccountAuthenticationAdobeClientToken(username, password, in_token);
  }

  /**
   * @return The user name value
   */

  public abstract String tokenUserName();

  /**
   * @return The password value
   */

  public abstract String tokenPassword();

  /**
   * @return The raw token value used to construct this token
   */

  public abstract String tokenRaw();
}
