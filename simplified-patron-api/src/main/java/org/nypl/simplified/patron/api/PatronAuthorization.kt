package org.nypl.simplified.patron.api

import org.joda.time.Instant

/**
 * Patron authorization.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

data class PatronAuthorization(
  val identifier: String,
  val expires: Instant?
)
