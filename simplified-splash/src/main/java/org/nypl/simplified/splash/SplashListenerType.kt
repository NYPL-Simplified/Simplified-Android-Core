package org.nypl.simplified.splash

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

interface SplashListenerType {

  val backgroundExecutor: ListeningScheduledExecutorService

  val splashEvents: ObservableType<SplashEvent>

  val profileController: ProfilesControllerType

  fun onSplashEULAIsProvided(): Boolean

  fun onSplashEULARequested(): EULAType

  fun onSplashEULACreateFragment()

  fun onSplashImageCreateFragment()

  fun onSplashOpenProfileSelector()

  fun onSplashOpenCatalog(account: AccountType)

  fun onSplashOpenProfileAnonymous()

}