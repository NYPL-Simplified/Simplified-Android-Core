package org.nypl.simplified.accounts.api

import java.net.URI

/**
 * A set of credentials bundled with the application.
 */

interface AccountBundledCredentialsType {

  /**
   * @return A read-only map of the bundled credentials
   */

  fun bundledCredentials(): Map<URI, AccountAuthenticationCredentials>

  /**
   * Obtain bundled credentials when creating account using the given provider.
   *
   * @param accountProvider The account provider URI
   * @return The bundled credentials, if any
   */

  fun bundledCredentialsFor(
    accountProvider: URI
  ): AccountAuthenticationCredentials?
}
