package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class AddressDetails(
  @field:Json(name = "city") val city: String,
  @field:Json(name = "line_1") val line_1: String,
  @field:Json(name = "state") val state: String,
  @field:Json(name = "zip") val zip: String
)
