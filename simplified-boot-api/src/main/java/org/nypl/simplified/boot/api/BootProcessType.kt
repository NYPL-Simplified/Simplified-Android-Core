package org.nypl.simplified.boot.api

/**
 * A function that sets up all of the required application services.
 */

interface BootProcessType<T> {

  /**
   * Set up application services, publishing events to `onProgress`.
   *
   * @return The initialized services
   */

  fun execute(onProgress: (BootEvent) -> Unit): T
}
