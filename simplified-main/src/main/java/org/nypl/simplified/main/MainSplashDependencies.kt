package org.nypl.simplified.main

import io.reactivex.Observable
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.ui.splash.SplashDependenciesType

class MainSplashDependencies : SplashDependenciesType {

  override val bootEvents: Observable<BootEvent> =
    MainApplication.application.servicesBootEvents
}
