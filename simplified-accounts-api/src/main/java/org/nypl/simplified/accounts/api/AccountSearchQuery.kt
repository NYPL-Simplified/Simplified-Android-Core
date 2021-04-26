package org.nypl.simplified.accounts.api

/**
 * A search query made to the library registry.
 */

data class AccountSearchQuery(
  val location: AccountGeoLocation?,
  val searchQuery: String,
  val includeTestingLibraries: Boolean
)
