package org.librarysimplified.services.api

/**
 * An interface that can be implemented by objects that provide access to the service directory.
 */

interface ServiceDirectoryProviderType {

  /**
   * A reference to the current service directory.
   */

  fun serviceDirectory(): ServiceDirectoryType

}