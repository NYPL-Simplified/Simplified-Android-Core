package org.nypl.simplified.viewer.spi

/**
 * Preferences for viewers. These preferences can influence which viewers will be selected
 * for a given book.
 */

data class ViewerPreferences(

  /**
   * An opaque, implementation-defined set of flags.
   */

  val flags: Map<String, Boolean>
)
