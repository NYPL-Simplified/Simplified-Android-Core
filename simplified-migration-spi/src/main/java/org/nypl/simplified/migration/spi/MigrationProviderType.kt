package org.nypl.simplified.migration.spi

/**
 * A provider of migrations from one version of the app to another.
 */

interface MigrationProviderType {

  /**
   * Create a new migration.
   */

  fun create(services: MigrationServiceDependencies): MigrationType
}
