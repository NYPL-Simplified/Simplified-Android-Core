package org.nypl.simplified.cardcreator.model

data class Dependent(
  val barcode: String,
  val id: Int,
  val name: String,
  val pin: String,
  val username: String
)
