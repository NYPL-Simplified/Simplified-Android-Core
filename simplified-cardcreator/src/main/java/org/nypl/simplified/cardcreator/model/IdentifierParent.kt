package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

sealed class IdentifierParent {

  data class BarcodeParent(
    @field:Json(name = "barcode") val barcode: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "username") val username: String,
    @field:Json(name = "pin") val pin: String
  ) : IdentifierParent()

  data class UsernameParent(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "parentUsername") val parentUsername: String,
    @field:Json(name = "username") val username: String,
    @field:Json(name = "pin") val pin: String
  ) : IdentifierParent()
}
