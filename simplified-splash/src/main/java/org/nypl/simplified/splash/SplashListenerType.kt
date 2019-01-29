package org.nypl.simplified.splash

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.eula.EULAType
import org.nypl.simplified.observable.ObservableType

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