package org.nypl.simplified.viewer.audiobook

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration

/**
 * A service for supplying Feedbooks configuration data.
 */

interface AudioBookFeedbooksServiceType {

  /**
   * Configuration information for the Feedbooks extension.
   */

  val configuration: FeedbooksPlayerExtensionConfiguration
}
