package org.nypl.simplified.accounts.api

import com.google.common.base.Preconditions
import java.net.URI

/**
 * A description of the details of authentication.
 */

sealed class AccountProviderAuthenticationDescription {

  companion object {

    /**
     * The type used to identify basic authentication.
     */

    const val BASIC_TYPE = "http://opds-spec.org/auth/basic"

    /**
     * The type used to identify COPPA age gate authentication.
     */

    const val COPPA_TYPE = "http://librarysimplified.org/terms/authentication/gate/coppa"

    /**
     * The type used to identify anonymous access (no authentication).
     */

    const val ANONYMOUS_TYPE = "http://librarysimplified.org/rel/auth/anonymous"
  }

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

    val under13: URI)
    : AccountProviderAuthenticationDescription() {
    init {
      Preconditions.checkState(
        this.greaterEqual13 != this.under13,
        "URIs ${this.greaterEqual13} and ${this.under13} must differ")
    }
  }

  /**
   * Basic authentication is required.
   */

  data class Basic(

    /**
     * The barcode format, if specified, such as "CODABAR". If this is unspecified, then
     * barcode scanning and displaying is not supported.
     */

    val barcodeFormat: String?,

    /**
     * The keyboard type used for barcode entry, such as "DEFAULT".
     */

    val keyboard: String?,

    /**
     * The maximum length of the password.
     */

    val passwordMaximumLength: Int,

    /**
     * The keyboard type used for password entry, such as "DEFAULT".
     */

    val passwordKeyboard: String?,

    /**
     * The description of the login dialog.
     */

    val description: String,

    /**
     * The labels that should be used for login forms. This is typically a map such as:
     *
     * ```
     * "login" -> "Barcode"
     * "password" -> "PIN"
     * ```
     */

    val labels: Map<String, String>)
    : AccountProviderAuthenticationDescription() {

    init {
      Preconditions.checkArgument(
        this.barcodeFormat?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
        "Barcode format ${this.barcodeFormat} must be uppercase")
      Preconditions.checkArgument(
        this.keyboard?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
        "Keyboard ${this.keyboard} must be uppercase")
      Preconditions.checkArgument(
        this.passwordKeyboard?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
        "Password keyboard ${this.passwordKeyboard} must be uppercase")
    }
  }

}
