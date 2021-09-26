package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class ValidateAddressRequest(
  @field:Json(name = "address") val address: Address,
)
