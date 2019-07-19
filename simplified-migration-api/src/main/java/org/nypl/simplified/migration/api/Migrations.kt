package org.nypl.simplified.migration.api

import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationProviderType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.ServiceLoader

/**
 * A class that runs the given set of migrations.
 */

class Migrations(
  private val serviceDependencies: MigrationServiceDependencies,
  private val services: () -> List<MigrationProviderType>) : MigrationsType {

  private var migrationServices: List<MigrationType>
  private val executor =
    NamedThreadPools.namedThreadPool(1, "migrations", 19)
  private val eventsObservable =
    Observable.create<MigrationEvent>()
  private val logger =
    LoggerFactory.getLogger(Migrations::class.java)

  init {
    try {
      val providers = this.services.invoke()
      this.logger.debug("loaded {} migration service providers", providers.size)
      this.migrationServices = providers.map { provider ->
        provider.create(this.serviceDependencies)
      }
    } catch (e: Exception) {
      this.logger.error("could not setup migration service provider: ", e)
      this.migrationServices = listOf()
    }
  }

  override val events: ObservableReadableType<MigrationEvent> =
    this.eventsObservable

  override fun anyNeedToRun(): Boolean {
    return this.migrationServices.any { service -> service.needsToRun() }
  }

  override fun runMigrations(): MigrationReport? {
    val migrationService =
      this.migrationServices.find { service -> service.needsToRun() }

    return migrationService?.let { service ->
      val subscription =
        migrationService.events.subscribe { event -> this.eventsObservable.send(event) }

       try {
        this.logger.debug("running migration service {}", migrationService.javaClass.canonicalName)
        service.run()
      } finally {
        subscription.unsubscribe()
      }
    }
  }

  companion object {

    /**
     * Create a default migration API, loading migration services from [ServiceLoader].
     */

    fun create(serviceDependencies: MigrationServiceDependencies): MigrationsType {
      return Migrations(serviceDependencies) {
        ServiceLoader.load(MigrationProviderType::class.java).toList()
      }
    }
  }
}