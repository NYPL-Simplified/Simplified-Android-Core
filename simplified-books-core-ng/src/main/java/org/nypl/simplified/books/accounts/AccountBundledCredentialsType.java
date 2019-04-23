package org.nypl.simplified.books.accounts;

import com.io7m.jfunctional.OptionType;

import java.net.URI;
import java.util.Map;

/**
 * A set of credentials bundled with the application.
 */

public interface AccountBundledCredentialsType {

  /**
   * @return A read-only map of the bundled credentials
   */

  Map<URI, AccountAuthenticationCredentials> bundledCredentials();

  /**
   * Obtain bundled credentials when creating account using the given provider.
   *
   * @param accountProvider The account provider URI
   * @return The bundled credentials, if any
   */

  OptionType<AccountAuthenticationCredentials> bundledCredentialsFor(
    URI accountProvider);

}
