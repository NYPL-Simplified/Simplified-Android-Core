package org.nypl.simplified.accounts.api

/**
 * The type of locations.
 */

sealed class AccountGeoLocation {

  /**
   * Convert the location to text.
   */

  abstract fun toText(): String

  /**
   * A latitude/longitude pair.
   */

  data class Coordinates(
    val longitude: Double,
    val latitude: Double
  ) : AccountGeoLocation() {
    override fun toText(): String =
      "$latitude,$longitude"
  }
}
