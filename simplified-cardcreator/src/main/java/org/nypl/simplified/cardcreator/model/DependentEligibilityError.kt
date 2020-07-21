package org.nypl.simplified.cardcreator.model

data class DependentEligibilityError(
  val message: String,
  val status: Int,
  val type: String
)
