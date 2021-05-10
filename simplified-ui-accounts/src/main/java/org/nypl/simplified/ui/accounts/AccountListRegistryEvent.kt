package org.nypl.simplified.ui.accounts

import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class AccountListRegistryEvent {

  object AccountCreated : AccountListRegistryEvent()

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : AccountListRegistryEvent()
}
