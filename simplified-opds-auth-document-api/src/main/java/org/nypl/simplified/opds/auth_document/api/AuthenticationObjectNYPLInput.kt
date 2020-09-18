package org.nypl.simplified.opds.auth_document.api

import com.google.common.base.Preconditions

/**
 * The NYPL's "input" extension to OPDS Authentication.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/Authentication-For-OPDS-Extensions#input-mechanisms"
 */

data class AuthenticationObjectNYPLInput(

  /**
   * The field type to which these definitions apply.
   */

  val fieldName: String,

  /**
   * The keyboard type that should be used for entry.
   */

  val keyboardType: String?,

  /**
   * The maximum length of the field, or `0` if there is no limit.
   */

  val maximumLength: Int,

  /**
   * The barcode format, such as "CODABAR".
   */

  val barcodeFormat: String?
) {

  init {
    Preconditions.checkArgument(
      this.fieldName.all { c -> c.isUpperCase() || c.isWhitespace() },
      "Field name ${this.fieldName} must be uppercase"
    )
    Preconditions.checkArgument(
      this.barcodeFormat?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
      "Barcode format ${this.barcodeFormat} must be uppercase"
    )
    Preconditions.checkArgument(
      this.keyboardType?.all { c -> c.isUpperCase() || c.isWhitespace() } ?: true,
      "Keyboard ${this.keyboardType} must be uppercase"
    )
  }
}
