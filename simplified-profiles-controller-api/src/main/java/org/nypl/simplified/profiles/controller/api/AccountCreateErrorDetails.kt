package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails

sealed class AccountCreateErrorDetails {

  data class AccountProviderResolutionFailed(
    val errorValue: AccountProviderResolutionErrorDetails?)
    : AccountCreateErrorDetails()

}
