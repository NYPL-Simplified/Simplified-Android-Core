package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class CreatePatronResponse(
  @field:Json(name = "type") val type: String,
  @field:Json(name = "status") val status: String,
  @field:Json(name = "title") val title: String,
  @field:Json(name = "detail") val detail: String,
  @field:Json(name = "debug_message") val debug_message: String,
  @field:Json(name = "message") val message: String,
  @field:Json(name = "patron_id") val patron_id: String,
  @field:Json(name = "barcode") val barcode: String,
  @field:Json(name = "username") val username: String,
  @field:Json(name = "pin") val pin: String,
  @field:Json(name = "temporary") val temporary: Boolean
)
