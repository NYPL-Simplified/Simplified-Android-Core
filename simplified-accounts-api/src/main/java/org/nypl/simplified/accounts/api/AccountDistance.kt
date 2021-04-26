package org.nypl.simplified.accounts.api

/**
 * The distance to a given library.
 *
 * @see "https://schema.org/Distance"
 */

data class AccountDistance(
  val length: Double,
  val unit: AccountDistanceUnit
) {

  /**
   * Convert the distance value to text.
   */

  fun toText(): String {
    return "${this.length} ${this.unit.displayName}"
  }
}
