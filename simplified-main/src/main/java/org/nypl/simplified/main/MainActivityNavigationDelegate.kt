package org.nypl.simplified.main

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.onboarding.OnboardingFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.splash.SplashFragment
import org.slf4j.LoggerFactory

internal class MainActivityNavigationDelegate(
  private val fragmentManager: FragmentManager,
  mainActivityViewModel: MainActivityViewModel
) : ProfilesNavigationControllerType by ProfilesNavigationController(
  fragmentManager, mainActivityViewModel
) {

  private val logger =
    LoggerFactory.getLogger(MainActivityNavigationDelegate::class.java)


  fun openSplashScreen(resultKey: String) {
    this.logger.debug("openSplashScreen")
    val splashFragment = SplashFragment.newInstance(resultKey)
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.commit {
      replace(R.id.mainFragmentHolder, splashFragment, "SPLASH_MAIN")
    }
  }

  fun openOnboarding(resultKey: String) {
    this.logger.debug("openOnboarding")
    val onboardingFragment = OnboardingFragment.newInstance(resultKey)
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.commit {
      replace(R.id.mainFragmentHolder, onboardingFragment)
    }
  }

  fun openProfileSelection() {
    this.logger.debug("openProfileSelection")
    val profilesFragment = ProfileSelectionFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.commit {
      replace(R.id.mainFragmentHolder, profilesFragment)
    }
  }

  fun openMainFragment() {
    this.logger.debug("openMainFragment")
    val mainFragment = MainFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.commit {
      replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
    }
  }

  fun handleNavigationCommand(command: MainActivityNavigationCommand) {
    when(command) {
      MainActivityNavigationCommand.ProfileCommand.OnProfileModificationCancelled ->
        this.onProfileModificationCancelled()
      MainActivityNavigationCommand.ProfileCommand.OnProfileModificationSucceeded ->
        this.onProfileModificationSucceeded()
      MainActivityNavigationCommand.ProfileCommand.OpenMain ->
        this.openMain()
      MainActivityNavigationCommand.ProfileCommand.OpenProfileCreate ->
        this.openProfileCreate()
      is MainActivityNavigationCommand.ProfileCommand.OpenProfileModify ->
        this.openProfileModify(command.id)
      MainActivityNavigationCommand.ProfileCommand.OpenProfileSelect ->
        this.openProfileSelect()
    }
  }
}
