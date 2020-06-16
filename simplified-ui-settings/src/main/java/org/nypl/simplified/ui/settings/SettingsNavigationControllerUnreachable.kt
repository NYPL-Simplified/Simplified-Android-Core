package org.nypl.simplified.ui.settings

import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * A basic settings navigation controller where every destination results in an exception.
 */

open class SettingsNavigationControllerUnreachable : SettingsNavigationControllerType {

  override fun openSettingsAbout() {
    throw UnreachableCodeException()
  }

  override fun openSettingsAccounts() {
    throw UnreachableCodeException()
  }

  override fun openSettingsAcknowledgements() {
    throw UnreachableCodeException()
  }

  override fun openSettingsEULA() {
    throw UnreachableCodeException()
  }

  override fun openSettingsFaq() {
    throw UnreachableCodeException()
  }

  override fun openSettingsLicense() {
    throw UnreachableCodeException()
  }

  override fun openSettingsVersion() {
    throw UnreachableCodeException()
  }

  override fun openSettingsCustomOPDS() {
    throw UnreachableCodeException()
  }

  override fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>) {
    throw UnreachableCodeException()
  }

  override fun openSettingsAccountRegistry() {
    throw UnreachableCodeException()
  }

  override fun popBackStack(): Boolean {
    throw UnreachableCodeException()
  }

  override fun backStackSize(): Int {
    throw UnreachableCodeException()
  }

  override fun openSettingsAccount(parameters: AccountFragmentParameters) {
    throw UnreachableCodeException()
  }
}
