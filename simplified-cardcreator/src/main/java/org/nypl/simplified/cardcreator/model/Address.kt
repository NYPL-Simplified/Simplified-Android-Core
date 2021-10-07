package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json
import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

data class Address(
  @field:Json(name = "line1") val line1: String,
  @field:Json(name = "line2") val line2: String = "",
  @field:Json(name = "city") val city: String,
  @field:Json(name = "state") val state: String,
  @field:Json(name = "zip") val zip: String,
  @field:Json(name = "isResidential") val isResidential: Boolean,
  @field:Json(name = "hasBeenValidated") val hasBeenValidated: Boolean = true
) {

  /**
   * Moshi might have set some missing fields to null instead of throwing an exception,
   * so we need to manually check every field is not null to prevent NullPointerExceptions where
   * no-one would expect them.
   */

  fun validate(): Address =
    this.apply {
      checkFieldNotNull(line1, "line1")
      checkFieldNotNull(line2, "line2")
      checkFieldNotNull(city, "city")
      checkFieldNotNull(state, "state")
      checkFieldNotNull(zip, "zip")
      checkFieldNotNull(isResidential, "isResidential")
      checkFieldNotNull(hasBeenValidated, "hasBeenValidated")
    }
}
