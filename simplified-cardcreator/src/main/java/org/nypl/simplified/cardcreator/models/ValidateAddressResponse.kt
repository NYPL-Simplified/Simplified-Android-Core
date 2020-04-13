package org.nypl.simplified.cardcreator.models

import com.squareup.moshi.Json

data class ValidateAddressResponse(
  @field:Json(name = "message") val message: String,
  @field:Json(name = "address") val address: OriginalAddress,
  @field:Json(name = "card_type") val card_type: String,
  @field:Json(name = "original_address") val original_address: OriginalAddress,
  @field:Json(name = "type") val type: String
)
