package org.nypl.simplified.migration.from3master

import org.nypl.simplified.migration.spi.MigrationProviderType
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType

/**
 * A provider of migrations from version 3.0 of the app (2019 pre-LFA master branch).
 *
 * Note: This class _MUST_ have a no-argument public constructor to work with [java.util.ServiceLoader].
 */

class MigrationFrom3MasterProvider(
  val environment: EnvironmentQueriesType
) : MigrationProviderType {

  /**
   * ServiceLoader constructor.
   */

  constructor() : this(EnvironmentQueriesDefault())

  private var strings: MigrationFrom3MasterStringResourcesType? = null

  /**
   * Set the string resources used for this provider. This is primarily for testing purposes.
   */

  fun setStrings(strings: MigrationFrom3MasterStringResourcesType) {
    this.strings = strings
  }

  override fun create(services: MigrationServiceDependencies): MigrationType {
    if (this.strings == null) {
      this.strings = MigrationFrom3MasterStrings(services.context.resources)
    }

    return MigrationFrom3Master(
      environment = this.environment,
      strings = this.strings!!,
      services = services
    )
  }
}
