package org.nypl.simplified.cardcreator.model

data class ISSOTokenData(
  val access_token: String,
  val expires_in: Int,
  val id_token: String,
  val scope: String,
  val token_type: String
)
