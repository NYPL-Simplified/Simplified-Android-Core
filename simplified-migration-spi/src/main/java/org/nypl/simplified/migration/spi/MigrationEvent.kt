package org.nypl.simplified.migration.spi

import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.PresentableType

/**
 * Something that occurred during migration.
 */

sealed class MigrationEvent : PresentableType {

  abstract val subject: Subject?

  /**
   * The subject of the notice.
   */

  enum class Subject {
    PROFILE,
    ACCOUNT,
    BOOK,
    BOOKMARK
  }

  /**
   * An informative event.
   */

  data class MigrationStepInProgress(
    override val message: String,
    override val subject: Subject? = null
  ) : MigrationEvent()

  /**
   * An informative event.
   */

  data class MigrationStepSucceeded(
    override val message: String,
    override val attributes: Map<String, String> = mapOf(),
    val causes: List<PresentableType> = listOf(),
    override val subject: Subject? = null
  ) : MigrationEvent()

  /**
   * An error that occurred during migration.
   */

  data class MigrationStepError(
    override val message: String,
    override val exception: Exception? = null,
    override val attributes: Map<String, String> = mapOf(),
    override val subject: Subject? = null
  ) : MigrationEvent(), PresentableErrorType
}
