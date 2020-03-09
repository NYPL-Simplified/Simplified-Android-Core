package org.nypl.simplified.migration.api

import io.reactivex.Observable
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationReport

/**
 * An interface that runs one or more migrations.
 */

interface MigrationsType {

  /**
   * An observable that publishes migration events.
   */

  val events: Observable<MigrationEvent>

  /**
   * @return A future that returns `true` if any migration needs to occur
   */

  fun anyNeedToRun(): Boolean

  /**
   * Run the first available migration that applies to the current application.
   *
   * @return `null` if no migrations need to run, or a migration report of the one that ran
   */

  fun runMigrations(): MigrationReport?
}
