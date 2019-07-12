package org.nypl.simplified.migration.spi

/**
 * A migration from one version of the app to another.
 */

interface MigrationType {

  /**
   * @return `true` if the migration detects that it needs to run
   */

  fun needsToRun(): Boolean

  /**
   * Run the migration.
   *
   * @return A report indicating what was achieved
   */

  fun run(): MigrationReport

}