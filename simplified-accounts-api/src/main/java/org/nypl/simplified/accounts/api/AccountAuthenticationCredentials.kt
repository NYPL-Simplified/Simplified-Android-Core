package org.nypl.simplified.accounts.api

/**
 * A set of account credentials.
 */

sealed class AccountAuthenticationCredentials {

  /**
   * @return The current credentials without any post-activation Adobe credentials
   */

  abstract fun withoutAdobePostActivationCredentials(): AccountAuthenticationCredentials

  /**
   * @return The current credentials without any post-activation Adobe credentials
   */

  abstract fun withAdobePreActivationCredentials(
    newCredentials: AccountAuthenticationAdobePreActivationCredentials
  ): AccountAuthenticationCredentials

  /**
   * The Adobe DRM credentials available to the account.
   */

  abstract val adobeCredentials: AccountAuthenticationAdobePreActivationCredentials?

  /**
   * The description of authentication method that was used. This corresponds to the [AccountProviderAuthenticationDescription.description] field.
   */

  abstract val authenticationDescription: String?

  /**
   * The user used basic authentication to authenticate.
   */

  data class Basic(
    val userName: AccountUsername,
    val password: AccountPassword,
    override val adobeCredentials: AccountAuthenticationAdobePreActivationCredentials?,
    override val authenticationDescription: String?
  ) : AccountAuthenticationCredentials() {
    override fun withoutAdobePostActivationCredentials(): AccountAuthenticationCredentials {
      return this.copy(
        adobeCredentials = this.adobeCredentials?.copy(postActivationCredentials = null)
      )
    }

    override fun withAdobePreActivationCredentials(
      newCredentials: AccountAuthenticationAdobePreActivationCredentials
    ): AccountAuthenticationCredentials {
      return this.copy(adobeCredentials = newCredentials)
    }
  }

  /**
   * The user used OAuth (with an intermediary) authentication to authenticate.
   */

  data class OAuthWithIntermediary(
    val accessToken: String,
    override val adobeCredentials: AccountAuthenticationAdobePreActivationCredentials?,
    override val authenticationDescription: String?
  ) : AccountAuthenticationCredentials() {
    override fun withoutAdobePostActivationCredentials(): AccountAuthenticationCredentials {
      return this.copy(
        adobeCredentials = this.adobeCredentials?.copy(postActivationCredentials = null)
      )
    }

    override fun withAdobePreActivationCredentials(
      newCredentials: AccountAuthenticationAdobePreActivationCredentials
    ): AccountAuthenticationCredentials {
      return this.copy(adobeCredentials = newCredentials)
    }
  }

  /**
   * The user used SAML 2.0 authentication to authenticate.
   */

  data class SAML2_0(
    val accessToken: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>,
    override val adobeCredentials: AccountAuthenticationAdobePreActivationCredentials?,
    override val authenticationDescription: String?
  ) : AccountAuthenticationCredentials() {
    override fun withoutAdobePostActivationCredentials(): AccountAuthenticationCredentials {
      return this.copy(
        adobeCredentials = this.adobeCredentials?.copy(postActivationCredentials = null)
      )
    }

    override fun withAdobePreActivationCredentials(
      newCredentials: AccountAuthenticationAdobePreActivationCredentials
    ): AccountAuthenticationCredentials {
      return this.copy(adobeCredentials = newCredentials)
    }
  }
}
