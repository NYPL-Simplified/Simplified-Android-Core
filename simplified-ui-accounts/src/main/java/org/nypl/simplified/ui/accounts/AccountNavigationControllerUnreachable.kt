package org.nypl.simplified.ui.accounts

import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * A basic account navigation controller where every destination results in an exception.
 */

open class AccountNavigationControllerUnreachable : AccountNavigationControllerType {
  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    throw UnreachableCodeException()
  }

  override fun openErrorPage(parameters: ErrorPageParameters) {
    throw UnreachableCodeException()
  }

  override fun openSettingsAccountRegistry() {
    throw UnreachableCodeException()
  }

  override fun popBackStack(): Boolean {
    throw UnreachableCodeException()
  }

  override fun popToRoot(): Boolean {
    throw UnreachableCodeException()
  }

  override fun backStackSize(): Int {
    throw UnreachableCodeException()
  }
}
