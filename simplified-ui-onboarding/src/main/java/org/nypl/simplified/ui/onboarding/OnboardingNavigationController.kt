package org.nypl.simplified.ui.onboarding

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.slf4j.LoggerFactory

internal class OnboardingNavigationController(
  private val fragmentManager: FragmentManager
) : NavigationControllerType, AccountNavigationControllerType {

  private val logger =
    LoggerFactory.getLogger(OnboardingNavigationController::class.java)

  override fun openSettingsAccountRegistry() {
    this.logger.debug("openSettingsAccountRegistry")
    val fragment = AccountListRegistryFragment()
    this.fragmentManager.commit {
      replace(R.id.onboarding_fragment_container, fragment)
      addToBackStack(null)
    }
  }

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    TODO("Not yet implemented")
  }

  override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
    TODO("Not yet implemented")
  }

  override fun openErrorPage(parameters: ErrorPageParameters) {
    TODO("Not yet implemented")
  }

  override fun openCatalogAfterAuthentication() {
    TODO("Not yet implemented")
  }

  override fun popBackStack(): Boolean {
    this.logger.debug("popBackStack")
    this.fragmentManager.popBackStack()
    return this.backStackSize() > 0
  }

  override fun popToRoot(): Boolean {
    this.logger.debug("popToRoot")
    if (this.backStackSize() == 0) {
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
