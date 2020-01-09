package org.nypl.simplified.patron.api

/**
 * Patron settings.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

data class PatronSettings(
  val synchronizeAnnotations: Boolean = false
)
