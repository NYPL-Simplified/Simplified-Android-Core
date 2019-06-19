package org.nypl.simplified.books.controller.api

import java.net.URI

/**
 * An interface providing localized strings for book revocation operations.
 */

interface BookRevokeStringResourcesType {

  /**
   * Performing revocation for format %1$s…
   */

  fun revokeFormatSpecific(type: String): String

  /**
   * A loan with availability %1$s is not revocable.
   */

  fun revokeServerNotifyNotRevocable(simpleName: String): String

  /**
   * Notifying the server via %1$s.
   */

  fun revokeServerNotifyURI(targetURI: URI): String

  /**
   * Credentials are required, but none are provided.
   */

  val revokeCredentialsRequired: String

  /**
   * Failed to delete the book database entry.
   */

  val revokeDeleteBookFailed: String

  /**
   * Deleting book database entry.
   */

  val revokeDeleteBook: String

  /**
   * Failed to save the server revocation feed.
   */

  val revokeServerNotifySavingEntryFailed: String

  /**
   * Saving revocation feed entry…
   */

  val revokeServerNotifySavingEntry: String

  /**
   * No URI was provided for server notification.
   */

  val revokeServerNotifyNoURI: String

  /**
   * Server returned a feed with a corrupted entry.
   */

  val revokeServerNotifyFeedCorrupt: String

  /**
   * Server returned a revocation feed with groups.
   */

  val revokeServerNotifyFeedWithGroups: String

  /**
   * Server returned an empty revocation feed.
   */

  val revokeServerNotifyFeedEmpty: String

  /**
   * Timed out waiting for server revocation feed.
   */

  val revokeServerNotifyFeedTimedOut: String

  /**
   * Failed to process server revocation feed.
   */

  val revokeServerNotifyFeedFailed: String

  /**
   * Successfully processed server revocation feed.
   */

  val revokeServerNotifyFeedOK: String

  /**
   * Processing server revocation feed…
   */

  val revokeServerNotifyProcessingFeed: String

  /**
   * Notifying server of revocation…
   */

  val revokeServerNotify: String

  /**
   * Revocation was cancelled.
   */

  val revokeBookCancelled: String

  /**
   * Executing Adobe ACS commands failed with an exception.
   */

  val revokeBookACSFailed: String

  /**
   * Adobe ACS commands failed with error code %1$s.
   */

  fun revokeBookACSConnectorFailed(errorCode: String): String

  /**
   * Timed out waiting for ACS.
   */

  val revokeACSTimedOut: String

  /**
   * Executed Adobe ACS revocation commands successfully.
   */

  val revokeACSExecuteOK: String

  /**
   * Executing Adobe ACS revocation commands…
   */

  val revokeACSExecute: String

  /**
   * Retrieved Adobe ACS device credentials.
   */

  val revokeACSGettingDeviceCredentialsOK: String

  /**
   * Adobe ACS device is not activated.
   */

  val revokeACSGettingDeviceCredentialsNotActivated: String

  /**
   * Retrieving Adobe ACS device credentials…
   */

  val revokeACSGettingDeviceCredentials: String

  /**
   * Failed to delete Adobe ACS rights information.
   */

  val revokeACSDeleteRightsFailed: String

  /**
   * Deleting Adobe ACS rights information…
   */

  val revokeACSDeleteRights: String

  /**
   * Adobe ACS believes that this loan is not returnable.
   */

  val revokeACSLoanNotReturnable: String

  /**
   * Adobe ACS is not supported.
   */

  val revokeACSLoanNotSupported: String

  /**
   * Revoking loan via Adobe ACS…
   */

  val revokeACSLoan: String

  /**
   * No specific operations needed for this format.
   */

  val revokeFormatNothingToDo: String

  /**
   * Performing format-specific revocation…
   */

  val revokeFormat: String

  /**
   * Failed to open the book database entry.
   */

  val revokeBookDatabaseLookupFailed: String

  /**
   * Opened book database entry successfully.
   */

  val revokeBookDatabaseLookupOK: String

  /**
   * Opening book database entry…
   */

  val revokeBookDatabaseLookup: String

  /**
   * Revocation started…
   */

  val revokeStarted: String

}
