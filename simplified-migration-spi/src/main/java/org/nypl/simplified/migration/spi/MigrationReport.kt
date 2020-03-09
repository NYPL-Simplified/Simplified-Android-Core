package org.nypl.simplified.migration.spi

import org.joda.time.LocalDateTime

/**
 * A report detailing everything that was achieved during a migration.
 */

data class MigrationReport(

  /**
   * The application name/version.
   */

  val application: String,

  /**
   * The name of the migration service that produced the report.
   */

  val migrationService: String,

  /**
   * The time the migration started.
   */

  val timestamp: LocalDateTime,

  /**
   * The events that occurred, if any.
   */

  val events: List<MigrationEvent>
)
