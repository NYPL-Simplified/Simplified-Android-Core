package org.nypl.simplified.cardcreator.model

data class DependentEligibilityData(
  val status: Int,
  val eligible: Boolean,
  val type: String,
  val message: String?,
  val description: String?
)
