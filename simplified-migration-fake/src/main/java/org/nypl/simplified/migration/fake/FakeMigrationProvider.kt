package org.nypl.simplified.migration.fake

import org.nypl.simplified.migration.spi.MigrationProviderType
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType

/**
 * A fake migration provider that always runs and takes ten seconds to do nothing.
 */

class FakeMigrationProvider : MigrationProviderType {
  override fun create(services: MigrationServiceDependencies): MigrationType {
    return FakeMigration()
  }
}