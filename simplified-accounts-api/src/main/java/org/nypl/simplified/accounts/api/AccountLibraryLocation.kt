package org.nypl.simplified.accounts.api

/**
 * The location of a library.
 */

data class AccountLibraryLocation(
  val location: AccountGeoLocation,
  val distance: AccountDistance?
)
