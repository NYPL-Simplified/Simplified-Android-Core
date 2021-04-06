package org.nypl.simplified.main

import androidx.fragment.app.FragmentManager
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.splash.SplashFragment
import org.slf4j.LoggerFactory

internal class StartupNavigationController (
  fragmentManager: FragmentManager,
  private val migrationReportEmail: String?
  ): BaseNavigationController(fragmentManager) {

  private val logger =
    LoggerFactory.getLogger(StartupNavigationController::class.java)

  fun openSplashScreen() {
    this.logger.debug("openSplashScreen")
    val splashFragment = SplashFragment.newInstance(migrationReportEmail)
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, splashFragment, "SPLASH_MAIN")
      .commit()
  }

  fun openProfileSelection() {
    this.logger.debug("openProfileSelection")
    val profilesFragment = ProfileSelectionFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, profilesFragment, "MAIN")
      .commit()
  }

  fun openMainFragment() {
    this.logger.debug("openMain")
    val mainFragment = MainFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
      .commit()
  }
}
