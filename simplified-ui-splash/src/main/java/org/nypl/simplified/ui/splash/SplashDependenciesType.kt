package org.nypl.simplified.ui.splash

import io.reactivex.Observable
import org.nypl.simplified.boot.api.BootEvent

interface SplashDependenciesType {

  /**
   * An observable value that publishes events as the application is booting.
   * The last event is delivered to new subscribers.
   */

  val bootEvents: Observable<BootEvent>
}
