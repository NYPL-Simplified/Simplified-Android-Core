package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class ValidateAddressRequest(
  val address: Address,
)
