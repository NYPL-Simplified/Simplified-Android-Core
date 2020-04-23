package org.nypl.simplified.cardcreator.models

import com.squareup.moshi.Json

data class ValidateUsernameResponse(
  @field:Json(name = "type") val type: String,
  @field:Json(name = "card_type") val card_type: String,
  @field:Json(name = "message") val message: String
)
