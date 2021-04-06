package org.nypl.simplified.ui.splash

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.migration.api.MigrationsType

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
   * The splash screen is completely finished.
   */

  fun onSplashDone()

  /**
   * The splash screen wants to run migrations, if necessary.
   */

  fun onSplashWantMigrations(): MigrationsType

  /**
   * The library selection screen was opened, and the user chose to pick a library.
   */

  fun onSplashLibrarySelectionWanted()

  /**
   * The library selection screen was opened, and the user chose to use the default collection.
   */

  fun onSplashLibrarySelectionNotWanted()
}
