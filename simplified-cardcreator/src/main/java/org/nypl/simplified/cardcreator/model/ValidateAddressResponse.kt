package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json
import org.nypl.simplified.cardcreator.utils.checkFieldNotNull
import java.lang.Exception

sealed class ValidateAddressResponse {

  data class ValidateAddressData(
    @field:Json(name = "address") val address: Address,
    @field:Json(name = "original_address") val originalAddress: Address,
  ) : ValidateAddressResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): ValidateAddressData =
      this.apply {
        checkFieldNotNull(address, "address")
        address.validate()
        checkFieldNotNull(originalAddress, "original_address")
        address.validate()
      }
  }

  data class AlternateAddressesError(
    @field:Json(name = "status") val status: Int,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "addresses") val addresses: List<Address>
  ) : ValidateAddressResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): AlternateAddressesError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
        checkFieldNotNull(addresses, "addresses")
        check(addresses.isNotEmpty())
        addresses.forEach(Address::validate)
      }
  }

  data class ValidateAddressError(
    @field:Json(name = "status") val status: Int,
    @field:Json(name = "type") val type: String,
  ) : ValidateAddressResponse() {

    val isUnrecognizedAddress: Boolean
      get() = type == "unrecognized-address"

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): ValidateAddressError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class ValidateAddressException(
    val exception: Exception
  ) : ValidateAddressResponse()
}
