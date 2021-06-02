package org.nypl.simplified.main

import android.content.res.Resources
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType

/**
 * Strings related to logout events.
 */

class MainLogoutStringResources(
  private val resources: Resources
) : AccountLogoutStringResourcesType {

  override val logoutUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override fun logoutDeactivatingDeviceAdobeFailed(
    errorCode: String,
    e: Throwable
  ): String {
    return this.resources.getString(R.string.logoutDeactivatingDeviceAdobeFailed, errorCode, e.javaClass.simpleName)
  }

  override val logoutDeviceDeactivationPostDeviceManagerFinished: String
    get() = this.resources.getString(R.string.logoutDeviceDeactivationPostDeviceManagerFinished)

  override val logoutDeviceDeactivationPostDeviceManager: String
    get() = this.resources.getString(R.string.logoutDeviceDeactivationPostDeviceManager)

  override val logoutDeactivatingDeviceAdobeUnsupported: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeUnsupported)

  override val logoutDeactivatingDeviceAdobeDeactivated: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeDeactivated)

  override val logoutDeactivatingDeviceAdobeNotActive: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeNotActive)

  override val logoutDeactivatingDeviceAdobe: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobe)

  override val logoutClearingBookDatabase: String
    get() = this.resources.getString(R.string.logoutClearingBookDatabase)

  override val logoutClearingBookDatabaseFailed: String
    get() = this.resources.getString(R.string.logoutClearingBookDatabaseFailed)

  override fun logoutUpdatingOPDSEntry(bookID: String): String {
    return this.resources.getString(R.string.logoutUpdatingOpdsEntry, bookID)
  }

  override val logoutNoAlternateLinkInDatabase: String
    get() = this.resources.getString(R.string.logoutNoAlternateLinkInDatabase)

  override val logoutOPDSFeedTimedOut: String
    get() = this.resources.getString(R.string.logoutOPDSFeedTimedOut)

  override val logoutOPDSFeedFailed: String
    get() = this.resources.getString(R.string.logoutOPDSFeedFailed)

  override val logoutOPDSFeedCorrupt: String
    get() = this.resources.getString(R.string.logoutOPDSFeedCorrupt)

  override val logoutOPDSFeedEmpty: String
    get() = this.resources.getString(R.string.logoutOPDSFeedEmpty)

  override val logoutOPDSFeedWithGroups: String
    get() = this.resources.getString(R.string.logoutOPDSFeedWithGroups)

  override val logoutNotLoggedIn: String
    get() = this.resources.getString(R.string.logoutNotLoggedIn)

  override val logoutStarted: String
    get() = this.resources.getString(R.string.logoutStarted)
}
