package org.nypl.simplified.books.audio

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration

/**
 * A service for supplying Feedbooks configuration data.
 */

interface AudioBookFeedbooksSecretServiceType : AudioBookSecretServiceType {

  /**
   * Configuration information for the Feedbooks extension.
   */

  val configuration: FeedbooksPlayerExtensionConfiguration
}
