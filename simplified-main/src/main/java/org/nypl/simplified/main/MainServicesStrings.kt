package org.nypl.simplified.main

import android.content.res.Resources
import org.nypl.simplified.boot.api.BootStringResourcesType

internal class MainServicesStrings(
  private val resources: Resources
) : BootStringResourcesType {

  val bootingProfileModificationFragmentService: String =
    this.resources.getString(R.string.bootingProfileModificationFragmentService)

  val bootingIdleTimerConfigurationService: String =
    this.resources.getString(R.string.bootingIdleTimerConfigurationService)

  val bootingCatalogConfiguration: String =
    this.resources.getString(R.string.bootingCatalogConfigurationService)

  val bootingSettingsConfiguration: String =
    this.resources.getString(R.string.bootingSettingsConfigurationService)

  val bootingBuildConfigurationService: String =
    this.resources.getString(R.string.bootingBuildConfigurationService)

  val bootingUIThreadService: String =
    this.resources.getString(R.string.bootingUIThreadService)

  val bootingUIBackgroundExecutor: String =
    this.resources.getString(R.string.bootingUIBackgroundExecutor)

  val bootingThemeService: String =
    this.resources.getString(R.string.bootingThemeService)

  val bootingProfileTimer: String =
    this.resources.getString(R.string.bootingProfileTimer)

  val bootingHTTPServer: String =
    this.resources.getString(R.string.bootingHTTPServer)

  val bootingClock: String =
    this.resources.getString(R.string.bootingClock)

  override val bootStarted: String =
    this.resources.getString(R.string.bootStarted)

  override val bootFailedGeneric: String =
    this.resources.getString(R.string.bootFailedGeneric)

  override val bootCompleted: String =
    this.resources.getString(R.string.bootingCompleted)

  fun bootingStrings(kind: String): String =
    this.resources.getString(R.string.bootingStrings, kind)

  val bootingNetworkConnectivity: String =
    this.resources.getString(R.string.bootingNetworkConnectivity)

  val bootingReaderBookmarkService: String =
    this.resources.getString(R.string.bootingReaderBookmarkService)

  val bootingBookController: String =
    this.resources.getString(R.string.bootingBookController)

  val bootingAuthenticationDocumentParsers: String =
    this.resources.getString(R.string.bootingAuthenticationDocumentParsers)

  val bootingPatronProfileParsers: String =
    this.resources.getString(R.string.bootingPatronProfileParsers)

  val bootingAnalytics: String =
    this.resources.getString(R.string.bootingAnalytics)

  val bootingFeedParser: String =
    this.resources.getString(R.string.bootingFeedParser)

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

  val initializingInstabug =
    this.resources.getString(R.string.bootingInstabug)
}
