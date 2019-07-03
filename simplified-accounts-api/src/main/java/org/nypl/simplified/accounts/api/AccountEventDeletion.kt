package org.nypl.simplified.accounts.api


sealed class AccountEventDeletion : AccountEvent() {

  data class AccountEventDeletionInProgress(
    val message: String)
    : AccountEventDeletion()

  data class AccountEventDeletionSucceeded(
    val id: AccountID)
    : AccountEventDeletion()

  data class AccountEventDeletionFailed(
    val message: String)
    : AccountEventDeletion()

}
