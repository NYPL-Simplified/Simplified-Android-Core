package org.nypl.simplified.migration.spi

import org.nypl.simplified.presentableerror.api.PresentableErrorType

/**
 * Something that occurred during migration.
 */

sealed class MigrationNotice {

  abstract val message: String

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

  data class MigrationInfo(
    override val message: String,
    override val subject: Subject? = null)
    : MigrationNotice()

  /**
   * An error that occurred during migration.
   */

  data class MigrationError(
    override val message: String,
    override val exception: Exception? = null,
    override val attributes: Map<String, String> = mapOf(),
    override val causes: List<PresentableErrorType> = listOf(),
    override val subject: Subject? = null)
    : MigrationNotice(), PresentableErrorType

}