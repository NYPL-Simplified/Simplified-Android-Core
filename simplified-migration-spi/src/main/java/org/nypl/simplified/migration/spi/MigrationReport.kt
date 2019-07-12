package org.nypl.simplified.migration.spi

/**
 * A report detailing everything that was achieved during a migration.
 */

data class MigrationReport(

  /**
   * The errors that occurred, if any.
   */

  val errors: List<MigrationError>)
