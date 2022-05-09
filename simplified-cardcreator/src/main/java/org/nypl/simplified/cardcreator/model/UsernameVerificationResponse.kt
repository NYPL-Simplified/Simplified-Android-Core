package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json
import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

sealed class UsernameVerificationResponse {

  data class UsernameVerificationSuccess(
    @field:Json(name = "type") val type: String,
  ) : UsernameVerificationResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): UsernameVerificationSuccess =
      this.apply {
        checkFieldNotNull(type, "type")
      }
  }

  data class UsernameVerificationError(
    @field:Json(name = "status") val status: Int,
    @field:Json(name = "type") val type: String
  ) : UsernameVerificationResponse() {

    val isInvalidUsername: Boolean
      get() = type == "invalid-username"

    val isUnavailableUsername: Boolean
      get() = type == "unavailable-username"

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): UsernameVerificationError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class UsernameVerificationException(
    val exception: Exception
  ) : UsernameVerificationResponse()
}
