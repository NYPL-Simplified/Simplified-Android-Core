package org.nypl.simplified.patron.api

/**
 * Patron user profile.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

data class PatronUserProfile(
  val settings: PatronSettings,
  val drm: List<PatronDRM>,
  val authorization: PatronAuthorization?)