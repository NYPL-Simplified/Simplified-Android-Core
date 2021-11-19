package org.nypl.simplified.viewer.epub.readium2

import android.app.Application
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.nypl.drm.core.AdobeAdeptFileAsset
import org.nypl.drm.core.AxisNowFileAsset
import org.nypl.drm.core.ContentProtectionProvider
import org.nypl.simplified.accessibility.AccessibilityServiceType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.asset.FileAsset
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

internal class Reader2ParametersAdapter(
  application: Application,
  currentProfile: ProfileReadableType,
  accessibilityService: AccessibilityServiceType,
) {
  private val logger =
    LoggerFactory.getLogger(Reader2ParametersAdapter::class.java)

  /**
   * Instantiate any content protections that might be needed for DRM...
   */

  private val contentProtections: List<ContentProtection> =
    ServiceLoader.load(ContentProtectionProvider::class.java).toList()
      .mapNotNull { provider ->
        this.logger.debug("instantiating content protection provider {}", provider.javaClass.canonicalName)
        provider.create(application)
      }

  /*
   * Load the most recently configured theme from the profile's preferences.
   */

  private val initialTheme =
    Reader2Themes.toSR2(
      currentProfile
        .preferences()
        .readerPreferences
    )

  private val spokenFeedbackEnabled: Boolean =
    accessibilityService.spokenFeedbackEnabled

  fun adapt(parameters: Reader2ActivityParameters): SR2ReaderParameters {
    val bookFile =
      when (val drmInfo = parameters.drmInfo) {
        is BookDRMInformation.ACS ->
          AdobeAdeptFileAsset(
            fileAsset = FileAsset(parameters.file),
            adobeRightsFile = drmInfo.rights?.first
          )
        is BookDRMInformation.AXIS ->
          AxisNowFileAsset(
            fileAsset = FileAsset(parameters.file),
            axisLicense = drmInfo.license,
            axisUserKey = drmInfo.userKey
          )
        else -> FileAsset(parameters.file)
      }

    return SR2ReaderParameters(
      contentProtections = contentProtections,
      bookFile = bookFile,
      bookId = parameters.entry.feedEntry.id,
      theme = initialTheme,
      controllers = SR2Controllers(),
      scrollingMode = if (this.spokenFeedbackEnabled) {
        SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
      } else {
        SR2ScrollingMode.SCROLLING_MODE_PAGINATED
      },
      pageNumberingMode = SR2PageNumberingMode.WHOLE_BOOK
    )
  }
}
