package org.nypl.simplified.cardcreator.model

import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

data class Dependent(
  val barcode: String,
  val id: Int,
  val name: String,
  val password: String,
  val username: String
) {

  /**
   * Moshi might have set some missing fields to null instead of throwing an exception,
   * so we need to manually check every field is not null to prevent NullPointerExceptions where
   * no-one would expect them.
   */

  fun validate(): Dependent =
    this.apply {
      checkFieldNotNull(barcode, "barcode")
      checkFieldNotNull(id, "id")
      checkFieldNotNull(name, "name")
      checkFieldNotNull(password, "password")
      checkFieldNotNull(username, "username")
    }
}
