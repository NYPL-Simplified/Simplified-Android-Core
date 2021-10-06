package org.nypl.simplified.cardcreator.model

import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

sealed class DependentEligibilityResponse {

  data class DependentEligibilityData(
    val eligible: Boolean,
  ) : DependentEligibilityResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): DependentEligibilityData =
      this.apply {
        checkFieldNotNull(eligible, "eligible")
      }
  }

  data class DependentEligibilityError(
    val status: Int,
    val type: String
  ) : DependentEligibilityResponse() {

    val isNotEligible: Boolean
      get() = type == "not-eligible-card"

    val isLimitReached: Boolean
      get() = type == "limit-reached"

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): DependentEligibilityError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class DependentEligibilityException(
    val exception: Exception
  ) : DependentEligibilityResponse()
}
