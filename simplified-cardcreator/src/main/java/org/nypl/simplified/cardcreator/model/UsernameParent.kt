package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class UsernameParent(
  @field:Json(name = "name") val name: String,
  @field:Json(name = "parentUsername") val parentUsername: String,
  @field:Json(name = "username") val username: String,
  @field:Json(name = "pin") val pin: String
)
