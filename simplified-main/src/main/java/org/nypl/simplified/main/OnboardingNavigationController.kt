package org.nypl.simplified.main

import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentManager
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.splash.SplashSelectionFragment
import org.slf4j.LoggerFactory

internal class OnboardingNavigationController(
  fragmentManager: FragmentManager,
  private val actionBar: ActionBar?
  ) : BaseNavigationController(fragmentManager) {

  private val logger =
    LoggerFactory.getLogger(OnboardingNavigationController::class.java)

  init {
    fragmentManager.addOnBackStackChangedListener {
      when (fragmentManager.fragments.last()) {
        is SplashSelectionFragment -> actionBar?.hide()
        is AccountListRegistryFragment -> actionBar?.show()
      }
    }
  }

  fun openOnboardingStartScreen() {
    this.logger.debug("openLibrarySelection")
    val fragment = SplashSelectionFragment.newInstance()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "SPLASH_MAIN")
      .commit()
  }

  fun openAccountListRegistry() {
    this.logger.debug("openAccountListRegistry")
    val fragment = AccountListRegistryFragment()
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  override fun popBackStack(): Boolean {
    this.logger.debug("popBackStack")
    this.fragmentManager.popBackStack()
    return this.backStackSize() > 0
  }

  override fun popToRoot(): Boolean {
    this.logger.debug("popToRoot")
    if (this.backStackSize() == 1) {
      return false
    }
    this.fragmentManager.popBackStack(
      null, FragmentManager.POP_BACK_STACK_INCLUSIVE
    )
    return true
  }

  override fun backStackSize(): Int {
    this.logger.debug("backStackSize")
    return this.fragmentManager.backStackEntryCount
  }
}
