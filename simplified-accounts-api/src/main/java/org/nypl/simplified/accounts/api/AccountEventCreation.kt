package org.nypl.simplified.accounts.api

sealed class AccountEventCreation : AccountEvent() {

  data class AccountEventCreationInProgress(
    val message: String)
    : AccountEventCreation()

  data class AccountEventCreationSucceeded(
    val id: AccountID)
    : AccountEventCreation()

  data class AccountEventCreationFailed(
    val message: String)
    : AccountEventCreation()

}
