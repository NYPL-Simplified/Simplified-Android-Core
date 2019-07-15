package org.nypl.simplified.migration.spi

/**
 * A report detailing everything that was achieved during a migration.
 */

data class MigrationReport(

  /**
   * The notices that occurred, if any.
   */

  val notices: List<MigrationNotice>)
