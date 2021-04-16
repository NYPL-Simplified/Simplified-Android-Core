package org.nypl.simplified.tests.mocking

import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import java.net.URI

class MockRevokeStringResources : BookRevokeStringResourcesType {

  override val revokeNotRevocable: String
    get() = "revokeNotRevocable"

  override val revokeUnexpectedException: String
    get() = "revokeUnexpectedException"

  override val revokeCredentialsRequired: String
    get() = "revokeCredentialsRequired"

  override val revokeACSTimedOut: String
    get() = "revokeACSTimedOut"

  override val revokeServerNotifySavingEntryFailed: String
    get() = "revokeServerNotifySavingEntryFailed"

  override val revokeServerNotifySavingEntry: String
    get() = "revokeServerNotifySavingEntry"

  override fun revokeFormatSpecific(type: String): String =
    "revokeFormatSpecific"

  override fun revokeServerNotifyNotRevocable(simpleName: String): String =
    "revokeServerNotifyNotRevocable"

  override fun revokeServerNotifyURI(targetURI: URI): String =
    "revokeServerNotifyURI"

  override val revokeDeleteBookFailed: String
    get() = "revokeDeleteBookFailed"
  override val revokeDeleteBook: String
    get() = "revokeDeleteBook"
  override val revokeServerNotifyNoURI: String
    get() = "revokeServerNotifyNoURI"
  override val revokeServerNotifyFeedCorrupt: String
    get() = "revokeServerNotifyFeedCorrupt"
  override val revokeServerNotifyFeedWithGroups: String
    get() = "revokeServerNotifyFeedWithGroups"
  override val revokeServerNotifyFeedEmpty: String
    get() = "revokeServerNotifyFeedEmpty"
  override val revokeServerNotifyFeedTimedOut: String
    get() = "revokeServerNotifyFeedTimedOut"
  override val revokeServerNotifyFeedFailed: String
    get() = "revokeServerNotifyFeedFailed"
  override val revokeServerNotifyFeedOK: String
    get() = "revokeServerNotifyFeedOK"
  override val revokeServerNotifyProcessingFeed: String
    get() = "revokeServerNotifyProcessingFeed"
  override val revokeServerNotify: String
    get() = "revokeServerNotify"
  override val revokeBookCancelled: String
    get() = "revokeBookCancelled"
  override val revokeBookACSFailed: String
    get() = "revokeBookACSFailed"

  override fun revokeBookACSConnectorFailed(errorCode: String): String =
    "revokeBookACSConnectorFailed"

  override val revokeACSExecuteOK: String
    get() = "revokeACSExecuteOK"
  override val revokeACSExecute: String
    get() = "revokeACSExecute"
  override val revokeACSGettingDeviceCredentialsOK: String
    get() = "revokeACSGettingDeviceCredentialsOK"
  override val revokeACSGettingDeviceCredentialsNotActivated: String
    get() = "revokeACSGettingDeviceCredentialsNotActivated"
  override val revokeACSGettingDeviceCredentials: String
    get() = "revokeACSGettingDeviceCredentials"
  override val revokeACSDeleteRightsFailed: String
    get() = "revokeACSDeleteRightsFailed"
  override val revokeACSDeleteRights: String
    get() = "revokeACSDeleteRights"
  override val revokeACSLoanNotReturnable: String
    get() = "revokeACSLoanNotReturnable"
  override val revokeACSLoanNotSupported: String
    get() = "revokeACSLoanNotSupported"
  override val revokeACSLoan: String
    get() = "revokeACSLoan"
  override val revokeFormatNothingToDo: String
    get() = "revokeFormatNothingToDo"
  override val revokeFormat: String
    get() = "revokeFormat"
  override val revokeBookDatabaseLookupFailed: String
    get() = "revokeBookDatabaseLookupFailed"
  override val revokeBookDatabaseLookupOK: String
    get() = "revokeBookDatabaseLookupOK"
  override val revokeBookDatabaseLookup: String
    get() = "revokeBookDatabaseLookup"
  override val revokeStarted: String
    get() = "revokeStarted"
}
