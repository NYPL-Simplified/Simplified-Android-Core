package org.nypl.simplified.accounts.api

import com.google.common.base.Preconditions
import java.io.Serializable
import java.net.URI

/**
 * A description of the details of authentication.
 */

sealed class AccountProviderAuthenticationDescription : Serializable {

  companion object {

    /**
     * The type used to identify basic authentication.
     */

    const val BASIC_TYPE =
      "http://opds-spec.org/auth/basic"

    /**
     * The type used to identify COPPA age gate authentication.
     */

    const val COPPA_TYPE =
      "http://librarysimplified.org/terms/authentication/gate/coppa"

    /**
     * The type used to identify anonymous access (no authentication).
     */

    const val ANONYMOUS_TYPE =
      "http://librarysimplified.org/rel/auth/anonymous"

    /**
     * The type used to identify OAuth with an intermediary. This is the authentication used
     * by projects such as Open eBooks.
     */

    const val OAUTH_INTERMEDIARY_TYPE =
      "http://librarysimplified.org/authtype/OAuth-with-intermediary"

    /**
     * The type used to identify SAML 2.0.
     */

    const val SAML_2_0_TYPE =
      "http://librarysimplified.org/authtype/SAML-2.0"
  }

  /**
   * `true` if this type of authentication involves a "login" operation
   */

  abstract val isLoginPossible: Boolean

  /**
   * The authentication description.
   */

  abstract val description: String

  /**
   * A COPPA age gate that redirects users that are thirteen or older to one URI, and under 13s
   * to a different URI.
   */

  data class COPPAAgeGate(

    /**
     * The feed URI for >= 13.
     */

    val greaterEqual13: URI,

    /**
     * The feed URI for < 13.
     */

    val under13: URI
  ) : AccountProviderAuthenticationDescription() {
    init {
      Preconditions.checkState(
        this.greaterEqual13 != this.under13,
        "URIs ${this.greaterEqual13} and ${this.under13} must differ"
      )
    }

    override val isLoginPossible: Boolean =
      false

    override val description: String =
      COPPA_TYPE
  }

  /**
   * Values used in the NYPL's "input" extension.
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/Authentication-For-OPDS-Extensions#keyboard"
   */

  enum class KeyboardInput {

    /**
     * The default keyboard for the platform.
     */

    DEFAULT,

    /**
     * A keyboard optimized for entering email addresses.
     */

    EMAIL_ADDRESS,

    /**
     * A numeric keypad.
     */

    NUMBER_PAD,

    /**
     * This server does not expect this input to be provided at all, and the client should not collect it.
     */

    NO_INPUT
  }

  /**
   * Basic authentication is required.
   */

  data class Basic(
    override val description: String,

    /**
     * The barcode format, if specified, such as "CODABAR". If this is unspecified, then
     * barcode scanning and displaying is not supported.
     */

    val barcodeFormat: String?,

    /**
     * The keyboard type used for barcode entry, such as "DEFAULT".
     *
     * Note: If the keyboard extension is missing or null, the value assumed should be DEFAULT, not NO_INPUT.
     */

    val keyboard: KeyboardInput,

    /**
     * The maximum length of the password.
     */

    val passwordMaximumLength: Int,

    /**
     * The keyboard type used for password entry, such as "DEFAULT".
     *
     * Note: If the keyboard extension is missing or null, the value assumed should be DEFAULT, not NO_INPUT.
     */

    val passwordKeyboard: KeyboardInput,

    /**
     * The labels that should be used for login forms. This is typically a map such as:
     *
     * ```
     * "login" -> "Barcode"
     * "password" -> "PIN"
     * ```
     */

    val labels: Map<String, String>,

    /**
     * The URI of the authentication logo.
     */

    val logoURI: URI?
  ) : AccountProviderAuthenticationDescription() {

    override val isLoginPossible: Boolean =
      true

    init {
      Preconditions.checkArgument(
        this.barcodeFormat?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
        "Barcode format ${this.barcodeFormat} must be uppercase"
      )
    }
  }

  /**
   * OAuth with an intermediary.
   */

  data class OAuthWithIntermediary(
    override val description: String,

    /**
     * The URI used to perform authentication.
     */

    val authenticate: URI,

    /**
     * The URI of the authentication logo.
     */

    val logoURI: URI?
  ) : AccountProviderAuthenticationDescription() {
    override val isLoginPossible: Boolean =
      true
  }

  /**
   * Anonymous authentication (equivalent to no authentication)
   */

  object Anonymous : AccountProviderAuthenticationDescription() {
    override val isLoginPossible: Boolean =
      false
    override val description: String =
      ANONYMOUS_TYPE
  }

  /**
   * SAML 2.0.
   */

  data class SAML2_0(
    override val description: String,

    /**
     * The URI used to perform authentication.
     */

    val authenticate: URI,

    /**
     * The URI of the authentication logo.
     */

    val logoURI: URI?
  ) : AccountProviderAuthenticationDescription() {
    override val isLoginPossible: Boolean =
      true
  }
}
