package org.nypl.simplified.cardcreator.models

import com.squareup.moshi.Json

data class OriginalAddress(
  @field:Json(name = "city") val city: String,
  @field:Json(name = "county") val county: String,
  @field:Json(name = "is_residential") val is_residential: Any,
  @field:Json(name = "line_1") val line_1: String,
  @field:Json(name = "line_2") val line_2: String,
  @field:Json(name = "state") val state: String,
  @field:Json(name = "zip") val zip: String
)
