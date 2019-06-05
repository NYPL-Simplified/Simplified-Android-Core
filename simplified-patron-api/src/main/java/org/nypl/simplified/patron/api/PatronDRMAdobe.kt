package org.nypl.simplified.patron.api

import java.net.URI

/**
 * Information related to Adobe DRM.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

data class PatronDRMAdobe(
  override val vendor: String,
  override val scheme: URI,
  val clientToken: String)
  : PatronDRM()
