package org.nypl.simplified.main

import android.content.res.Resources
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import java.net.URI

/**
 * Status strings for revocation.
 */

class MainCatalogBookRevokeStrings(
  private val resources: Resources
) : BookRevokeStringResourcesType {

  override val revokeNotRevocable: String
    get() = this.resources.getString(R.string.revokeNotRevocable)

  override val revokeUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override val revokeCredentialsRequired: String
    get() = this.resources.getString(R.string.revokeCredentialsRequired)

  override fun revokeFormatSpecific(type: String): String =
    this.resources.getString(R.string.revokeFormatSpecific, type)

  override fun revokeServerNotifyNotRevocable(simpleName: String): String =
    this.resources.getString(R.string.revokeServerNotifyNotRevocable, simpleName)

  override fun revokeServerNotifyURI(targetURI: URI): String =
    this.resources.getString(R.string.revokeServerNotifyURI, targetURI)

  override val revokeDeleteBookFailed: String
    get() = this.resources.getString(R.string.revokeDeleteBookFailed)

  override val revokeDeleteBook: String
    get() = this.resources.getString(R.string.revokeDeleteBook)

  override val revokeServerNotifySavingEntryFailed: String
    get() = this.resources.getString(R.string.revokeServerNotifySavingEntryFailed)

  override val revokeServerNotifySavingEntry: String
    get() = this.resources.getString(R.string.revokeServerNotifySavingEntry)

  override val revokeServerNotifyNoURI: String
    get() = this.resources.getString(R.string.revokeServerNotifyNoURI)

  override val revokeServerNotifyFeedCorrupt: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedCorrupt)

  override val revokeServerNotifyFeedWithGroups: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedWithGroups)

  override val revokeServerNotifyFeedEmpty: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedEmpty)

  override val revokeServerNotifyFeedTimedOut: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedTimedOut)

  override val revokeServerNotifyFeedFailed: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedFailed)

  override val revokeServerNotifyFeedOK: String
    get() = this.resources.getString(R.string.revokeServerNotifyFeedOK)

  override val revokeServerNotifyProcessingFeed: String
    get() = this.resources.getString(R.string.revokeServerNotifyProcessingFeed)

  override val revokeServerNotify: String
    get() = this.resources.getString(R.string.revokeServerNotify)

  override val revokeACSTimedOut: String
    get() = this.resources.getString(R.string.revokeACSTimedOut)

  override val revokeBookCancelled: String
    get() = this.resources.getString(R.string.revokeBookCancelled)

  override val revokeBookACSFailed: String
    get() = this.resources.getString(R.string.revokeBookACSFailed)

  override fun revokeBookACSConnectorFailed(errorCode: String): String =
    this.resources.getString(R.string.revokeBookACSConnectorFailed)

  override val revokeACSExecuteOK: String
    get() = this.resources.getString(R.string.revokeACSExecuteOK)

  override val revokeACSExecute: String
    get() = this.resources.getString(R.string.revokeACSExecute)

  override val revokeACSGettingDeviceCredentialsOK: String
    get() = this.resources.getString(R.string.revokeACSGettingDeviceCredentialsOK)

  override val revokeACSGettingDeviceCredentialsNotActivated: String
    get() = this.resources.getString(R.string.revokeACSGettingDeviceCredentialsNotActivated)

  override val revokeACSGettingDeviceCredentials: String
    get() = this.resources.getString(R.string.revokeACSGettingDeviceCredentials)

  override val revokeACSDeleteRightsFailed: String
    get() = this.resources.getString(R.string.revokeACSDeleteRightsFailed)

  override val revokeACSDeleteRights: String
    get() = this.resources.getString(R.string.revokeACSDeleteRights)

  override val revokeACSLoanNotReturnable: String
    get() = this.resources.getString(R.string.revokeACSLoanNotReturnable)

  override val revokeACSLoanNotSupported: String
    get() = this.resources.getString(R.string.revokeACSLoanNotSupported)

  override val revokeACSLoan: String
    get() = this.resources.getString(R.string.revokeACSLoan)

  override val revokeFormatNothingToDo: String
    get() = this.resources.getString(R.string.revokeFormatNothingToDo)

  override val revokeFormat: String
    get() = this.resources.getString(R.string.revokeFormat)

  override val revokeBookDatabaseLookupOK: String
    get() = this.resources.getString(R.string.revokeBookDatabaseLookupOK)

  override val revokeBookDatabaseLookup: String
    get() = this.resources.getString(R.string.revokeBookDatabaseLookup)

  override val revokeStarted: String
    get() = this.resources.getString(R.string.revokeStarted)
}
