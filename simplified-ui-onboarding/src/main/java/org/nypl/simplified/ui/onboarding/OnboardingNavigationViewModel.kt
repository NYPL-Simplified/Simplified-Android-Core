package org.nypl.simplified.ui.onboarding

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import org.nypl.simplified.navigation.api.NavigationControllerViewModel
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

class OnboardingNavigationViewModel :
  NavigationControllerViewModel(),
  AccountNavigationControllerType {

  val commandQueue: UnicastWorkSubject<OnboardingNavigationCommand> =
    UnicastWorkSubject.create()

  init {
    this.updateNavigationController(
      AccountNavigationControllerType::class.java,
      this
    )
  }

  override fun openSettingsAccountRegistry() {
    val command = OnboardingNavigationCommand.AccountNavigationCommand.OpenSettingsAccountRegistry
    commandQueue.onNext(command)
  }

  override fun onAccountCreated() {
    val command = OnboardingNavigationCommand.AccountNavigationCommand.OnAccountCreated
    commandQueue.onNext(command)
  }

  override fun onSAMLEventAccessTokenObtained() {
    val command = OnboardingNavigationCommand.AccountNavigationCommand.OnSAMLCommandAccessTokenObtained
    commandQueue.onNext(command)
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
}
