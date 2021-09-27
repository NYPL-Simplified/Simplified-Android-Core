package org.nypl.simplified.accounts.api

/**
 * A unit of measurement for a distance.
 *
 * @see "https://schema.org/Distance"
 */

enum class AccountDistanceUnit(
  val displayName: String
) {

  /**
   * Kilometers.
   */

  KILOMETERS("km")
}
