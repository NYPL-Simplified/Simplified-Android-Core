package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class BarcodeParent(
  @field:Json(name = "barcode") val barcode: String,
  @field:Json(name = "name") val name: String,
  @field:Json(name = "username") val username: String,
  @field:Json(name = "pin") val pin: String
)
