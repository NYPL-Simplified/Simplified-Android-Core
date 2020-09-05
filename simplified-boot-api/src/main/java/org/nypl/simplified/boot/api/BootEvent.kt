package org.nypl.simplified.boot.api

import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.PresentableType

/**
 * The type of events that are published while the application boots.
 */

sealed class BootEvent {

  /**
   * Booting is in progress.
   */

  data class BootInProgress(
    override val message: String,
    override val attributes: Map<String, String> = mapOf()
  ) : BootEvent(), PresentableType

  /**
   * Booting has completed.
   */

  data class BootCompleted(
    override val message: String
  ) : BootEvent(), PresentableType

  /**
   * Booting has failed.
   */

  data class BootFailed(
    override val message: String,
    override val exception: Exception,
    override val attributes: Map<String, String> = mapOf()
  ) : BootEvent(), PresentableErrorType
}
