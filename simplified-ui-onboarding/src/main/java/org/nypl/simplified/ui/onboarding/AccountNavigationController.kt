package org.nypl.simplified.ui.onboarding

import androidx.fragment.app.FragmentManager
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/*
 * A custom navigation controller used by the onboarding. It's
 * only capable of moving to the error page, or popping the back stack.
 */

internal class AccountNavigationController(
  private val fragmentManager: FragmentManager,
  private val onPopBackStack: () -> Unit
) : AccountNavigationControllerType {

  override fun popBackStack(): Boolean {
    onPopBackStack()
    return true
  }

  override fun popToRoot(): Boolean {
    onPopBackStack()
    return true
  }

  override fun backStackSize(): Int {
    return fragmentManager.backStackEntryCount
  }

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    throw UnreachableCodeException()
  }

  override fun openErrorPage(parameters: ErrorPageParameters) {
    throw UnreachableCodeException()
  }

  override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
    throw UnreachableCodeException()
  }

  override fun openSettingsAccountRegistry() {
    throw UnreachableCodeException()
  }

  override fun openCatalogAfterAuthentication() {
    throw UnreachableCodeException()
  }
}
