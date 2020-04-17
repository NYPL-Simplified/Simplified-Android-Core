package org.nypl.simplified.books.audio

/**
 * A service that may be registered in the application service directory to provide
 * Overdrive-related secrets.
 */

interface AudioBookOverdriveSecretServiceType : AudioBookSecretServiceType {

  /**
   * The client key.
   */

  val clientKey: String

  /**
   * The client secret.
   */

  val clientPass: String
}
