package org.nypl.simplified.accounts.api

import java.io.File
import java.net.URI

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
   * Determine the correct catalog URI to use for readers of a given age. This is analogous
   * to [AccountProviderType.catalogURIForAge] except that it also considers the
   * [AccountPreferences.catalogURIOverride] value if it is present.
   *
   * @param age The age of the reader
   * @return The correct catalog URI for the given age
   */

  fun catalogURIForAge(age: Int): URI

  /**
   * Determine if the provided feedURI points to the root of this account's catalog.
   */

  fun feedIsRoot(feedURI: URI): Boolean {
    return when (val auth = this.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        auth.greaterEqual13 == feedURI || auth.under13 == feedURI
      is AccountProviderAuthenticationDescription.SAML2_0,
      AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.Basic,
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary ->
        this.provider.catalogURI == feedURI || this.preferences.catalogURIOverride == feedURI
    }
  }

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
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> true
      is AccountProviderAuthenticationDescription.SAML2_0 -> true
      is AccountProviderAuthenticationDescription.Anonymous -> false
    }
}
