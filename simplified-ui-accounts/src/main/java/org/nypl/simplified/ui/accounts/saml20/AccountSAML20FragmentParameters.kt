package org.nypl.simplified.ui.accounts.saml20

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import java.io.Serializable

/**
 * Parameters for the SAML 2.0 fragment.
 */

data class AccountSAML20FragmentParameters(
  val accountID: AccountID,
  val authenticationDescription: AccountProviderAuthenticationDescription.SAML2_0
) : Serializable
