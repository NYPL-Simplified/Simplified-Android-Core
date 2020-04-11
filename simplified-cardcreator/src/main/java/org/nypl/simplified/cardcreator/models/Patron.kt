package org.nypl.simplified.cardcreator.models

import com.squareup.moshi.Json

data class Patron(
  @field:Json(name = "address") val address: AddressDetails,
  @field:Json(name = "email") val email: String,
  @field:Json(name = "name") val name: String,
  @field:Json(name = "pin") val pin: String,
  @field:Json(name = "username") val username: String,
  @field:Json(name = "work_or_school_address") val work_or_school_address: AddressDetails
)
