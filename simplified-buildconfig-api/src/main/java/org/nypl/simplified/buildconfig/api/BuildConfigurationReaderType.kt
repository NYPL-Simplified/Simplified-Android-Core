package org.nypl.simplified.buildconfig.api

/**
 * Configuration values related to readers/viewers.
 */

interface BuildConfigurationReaderType {

  /**
   * Allow access to external URLs in readers.
   */

  val allowExternalReaderLinks : Boolean
}
