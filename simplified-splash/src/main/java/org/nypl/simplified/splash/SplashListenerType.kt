package org.nypl.simplified.splash

import com.google.common.util.concurrent.ListenableFuture
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.observable.ObservableReadableType
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

  fun onSplashWantBootEvents(): ObservableReadableType<BootEvent>

  /**
   * @return `true` if a EULA document is bundled into the application
   */

  fun onSplashEULAIsProvided(): Boolean

  /**
   * @return The EULA, assuming that [onSplashEULAIsProvided] returned `true`
   */

  fun onSplashEULARequested(): EULAType

  /**
   * The splash screen wants the current activity to finish and the profile selection screen
   * to be opened.
   */

  fun onSplashOpenProfileSelector()

  /**
   * The splash screen wants the current activity to finish and the catalog to be opened.
   */

  fun onSplashOpenCatalog()

  /**
   * The splash screen wants the application to switch to the anonymous profile.
   */

  fun onSplashOpenProfileAnonymous()

  /**
   * The splash screen wants to know what mode the profile system is in.
   */

  fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled

}