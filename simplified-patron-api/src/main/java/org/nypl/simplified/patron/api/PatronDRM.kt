package org.nypl.simplified.patron.api

import java.net.URI

/**
 * Information related to DRM.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

abstract class PatronDRM {
  abstract val vendor: String
  abstract val scheme: URI
}