package org.nypl.simplified.ui.splash

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import io.reactivex.Observable
import org.librarysimplified.documents.EULAType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.profiles.api.ProfilesDatabaseType

/**
 * A listener interface for the splash screen fragment.
 *
 * This interface is expected to be implemented by the activity hosting the fragment.
 *
 * @see SplashFragment
 */

interface SplashListenerType {

  /**
   * The splash screen wants access to a future that represents the application startup
   * procedure.
   */

  fun onSplashWantBootFuture(): ListenableFuture<*>

  /**
   * The splash screen wants access to an observable that publishes application startup events.
   */

  fun onSplashWantBootEvents(): Observable<BootEvent>

  /**
   * @return `true` if a EULA document is bundled into the application
   */

  fun onSplashEULAIsProvided(): Boolean

  /**
   * @return The EULA, assuming that [onSplashEULAIsProvided] returned `true`
   */

  fun onSplashEULARequested(): EULAType

  /**
   * The splash screen is completely finished.
   */

  fun onSplashDone()

  /**
   * The splash screen wants the application to switch to the anonymous profile.
   */

  fun onSplashOpenProfileAnonymous()

  /**
   * The splash screen wants to know what mode the profile system is in.
   */

  fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled

  /**
   * The splash screen wants to run migrations, if necessary.
   */

  fun onSplashWantMigrations(): MigrationsType

  /**
   * The splash screen wants to execute one or more long-running migrations and therefore
   * needs access to an executor.
   */

  fun onSplashWantMigrationExecutor(): ListeningExecutorService

  /**
   * A migration finished executing and produced a report.
   */

  fun onSplashMigrationReport(report: MigrationReport)

  /**
   * The library selection screen was opened, and the user chose to pick a library.
   */

  fun onSplashLibrarySelectionWanted()

  /**
   * The library selection screen was opened, and the user chose to use the default collection.
   */

  fun onSplashLibrarySelectionNotWanted()
}
