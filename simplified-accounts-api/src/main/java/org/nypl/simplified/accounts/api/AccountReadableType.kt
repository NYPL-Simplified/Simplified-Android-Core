package org.nypl.simplified.accounts.api

import java.io.File

/**
 * The read-only interface exposed by accounts.
 *
 * An account aggregates a set of credentials and a book database.
 * Account are assigned monotonically increasing identifiers by the
 * application, but the identifiers themselves carry no meaning. It is
 * an error to depend on the values of identifiers for any kind of
 * program logic.
 */

interface AccountReadableType {

  /**
   * @return The account ID
   */

  val id: AccountID

  /**
   * @return The full path to the on-disk directory storing data for this account
   */

  val directory: File

  /**
   * @return The account provider associated with this account
   */

  val provider: AccountProviderType

  /**
   * @return The current state of the account with respect to logging in/out
   */

  val loginState: AccountLoginState

  /**
   * @return The account preferences
   */

  val preferences: AccountPreferences

  /**
   * @return `true` if the account requires credentials to perform operations
   */

  val requiresCredentials: Boolean
    get() = when (this.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> false
      is AccountProviderAuthenticationDescription.Basic -> true
      null -> false
    }
}
