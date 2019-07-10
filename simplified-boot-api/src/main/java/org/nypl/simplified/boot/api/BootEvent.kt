package org.nypl.simplified.boot.api

/**
 * The type of events that are published while the application boots.
 */

sealed class BootEvent {

  /**
   * Booting is in progress.
   */

  data class BootInProgress(
    val message: String)
    : BootEvent()

  /**
   * Booting has completed.
   */

  data class BootCompleted(
    val message: String)
    : BootEvent()

  /**
   * Booting has failed.
   */

  data class BootFailed(
    val message: String,
    val exception: Exception)
    : BootEvent()
}
