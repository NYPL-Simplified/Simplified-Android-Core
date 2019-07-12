package org.nypl.simplified.migration.spi

import org.nypl.simplified.presentableerror.api.PresentableErrorType
import java.lang.Exception

/**
 * An error that occurred during migration.
 */

data class MigrationError(
  override val message: String,
  override val exception: Exception? = null,
  override val attributes: Map<String, String> = mapOf(),
  override val causes: List<PresentableErrorType> = listOf())
  : PresentableErrorType
