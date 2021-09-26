package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json
import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

sealed class CreatePatronResponse {

  data class CreatePatronData(
    @field:Json(name = "patronId") val patronId: String,
    @field:Json(name = "barcode") val barcode: String,
    @field:Json(name = "username") val username: String,
    @field:Json(name = "password") val password: String,
  ) : CreatePatronResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): CreatePatronData =
      this.apply {
        checkFieldNotNull(patronId, "patronId")
        checkFieldNotNull(barcode, "barcode")
        checkFieldNotNull(username, "username")
        checkFieldNotNull(password, "password")
      }
  }

  data class CreatePatronHttpError(
    @field:Json(name = "status") val status: Int,
    @field:Json(name = "type") val type: String,
  ) : CreatePatronResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): CreatePatronHttpError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class CreatePatronException(
    val exception: Exception
  ) : CreatePatronResponse()
}
