package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class Address(
  @field:Json(name = "address") val address: AddressDetails,
  @field:Json(name = "is_work_or_school_address") val is_work_or_school_address: Boolean
)
