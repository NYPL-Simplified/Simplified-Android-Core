package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json
import org.nypl.simplified.cardcreator.utils.checkFieldNotNull
import java.lang.Exception

sealed class ValidateUsernameResponse {

  data class ValidateUsernameData(
    @field:Json(name = "type") val type: String,
  ) : ValidateUsernameResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): ValidateUsernameData =
      this.apply {
        checkFieldNotNull(type, "type")
      }
  }

  data class ValidateUsernameError(
    @field:Json(name = "status") val status: Int,
    @field:Json(name = "type") val type: String
  ) : ValidateUsernameResponse() {

    val isInvalidUsername: Boolean
      get() = type == "invalid-username"

    val isUnavailableUsername: Boolean
      get() = type == "unavailable-username"

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): ValidateUsernameError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class ValidateUsernameException(
    val exception: Exception
  ) : ValidateUsernameResponse()
}
