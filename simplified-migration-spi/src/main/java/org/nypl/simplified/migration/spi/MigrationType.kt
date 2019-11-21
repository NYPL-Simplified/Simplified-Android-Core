package org.nypl.simplified.migration.spi

import io.reactivex.Observable

/**
 * A migration from one version of the app to another.
 */

interface MigrationType {

  /**
   * Events published my the migration.
   */

  val events: Observable<MigrationEvent>

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