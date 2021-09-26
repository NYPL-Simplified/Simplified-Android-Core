package org.nypl.simplified.cardcreator.model

import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

data class Parent(
  val barcode: String,
  val dependents: String,
  val updated: Boolean
) {

  /**
   * Moshi might have set some missing fields to null instead of throwing an exception,
   * so we need to manually check every field is not null to prevent NullPointerExceptions where
   * no-one would expect them.
   */

  fun validate(): Parent =
    this.apply {
      checkFieldNotNull(barcode, "barcode")
      checkFieldNotNull(dependents, "dependents")
      checkFieldNotNull(updated, "updated")
    }
}
