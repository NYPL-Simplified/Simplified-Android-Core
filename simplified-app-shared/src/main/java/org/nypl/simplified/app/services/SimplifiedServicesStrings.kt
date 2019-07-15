package org.nypl.simplified.app.services

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.boot.api.BootStringResourcesType

class SimplifiedServicesStrings(private val resources: Resources): BootStringResourcesType {

  override val bootStarted: String =
    this.resources.getString(R.string.bootStarted)

  override val bootFailedGeneric: String =
    this.resources.getString(R.string.bootFailedGeneric)

  override val bootCompleted: String =
    this.resources.getString(R.string.bootingCompleted)

  val bootingHelpstack: String =
    this.resources.getString(R.string.bootingHelpstack)

  val bootingNetworkConnectivity: String =
    this.resources.getString(R.string.bootingNetworkConnectivity)

  val bootingReaderBookmarkService: String =
    this.resources.getString(R.string.bootingReaderBookmarkService)

  val bootingBookController: String =
    this.resources.getString(R.string.bootingBookController)

  val bootingPatronProfileParsers: String =
    this.resources.getString(R.string.bootingPatronProfileParsers)

  val bootingAnalytics: String =
    this.resources.getString(R.string.bootingAnalytics)

  val bootingFeedLoader: String =
    this.resources.getString(R.string.bootingFeedLoader)

  val bootingBundledContent: String =
    this.resources.getString(R.string.bootingBundledContent)

  val bootingProfilesDatabase: String =
    this.resources.getString(R.string.bootingProfilesDatabase)

  val bootingCredentialStore: String =
    this.resources.getString(R.string.bootingCredentialStore)

  val bootingBundledCredentials: String =
    this.resources.getString(R.string.bootingBundledCredentials)

  val bootingAccountProviders: String =
    this.resources.getString(R.string.bootingAccountProviders)

  val bootingDocumentStore: String =
    this.resources.getString(R.string.bootingDocumentStore)

  val bootingEPUBLoader: String =
    this.resources.getString(R.string.bootingEPUBLoader)

  val bootingLocalImageLoader: String =
    this.resources.getString(R.string.bootingLocalImageLoader)

  val bootingCoverProvider: String =
    this.resources.getString(R.string.bootingCoverProvider)

  val bootingCoverBadgeProvider: String =
    this.resources.getString(R.string.bootingCoverBadgeProvider)

  val bootingCoverGenerator: String =
    this.resources.getString(R.string.bootingCoverGenerator)

  val bootingTenPrint: String =
    this.resources.getString(R.string.bootingTenPrint)

  val bootingBrandingServices: String =
    this.resources.getString(R.string.bootingBrandingServices)

  val bootingBookRegistry: String =
    this.resources.getString(R.string.bootingBookRegistry)

  val bootingDownloadService: String =
    this.resources.getString(R.string.bootingDownloadService)

  val bootingDirectories: String =
    this.resources.getString(R.string.bootingDirectories)

  val bootingHTTP: String =
    this.resources.getString(R.string.bootingHTTP)

  val bootingScreenSize: String =
    this.resources.getString(R.string.bootingScreenSize)

  val bootingAdobeDRM: String =
    this.resources.getString(R.string.bootingAdobeDRM)

  val bootingBugsnag =
    this.resources.getString(R.string.bootingBugSnag)

  val bootingNotificationsService =
    this.resources.getString(R.string.bootingNotificationsService)

}