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

  fun onSplashLibrarySelectionWanted()

  /**
   * The library selection screen was opened, and the user chose to use the default collection.
   */

  fun onSplashLibrarySelectionNotWanted()
}
