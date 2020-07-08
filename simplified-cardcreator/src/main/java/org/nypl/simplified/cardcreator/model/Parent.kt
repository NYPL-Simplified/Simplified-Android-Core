package org.nypl.simplified.cardcreator.model

data class Parent(
  val barcode: String,
  val dependents: String,
  val updated: Boolean
)
