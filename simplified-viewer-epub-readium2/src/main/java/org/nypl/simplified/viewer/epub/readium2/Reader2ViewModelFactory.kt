package org.nypl.simplified.viewer.epub.readium2

import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType

internal class Reader2ViewModelFactory(
  private val applicationInfo: ApplicationInfo,
  private val parameters: Reader2ActivityParameters,
  private val services: ServiceDirectoryType,
  private val readerModel: SR2ReaderViewModel,
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return when {
      (modelClass.isAssignableFrom(Reader2ViewModel::class.java)) -> {
        val profilesController =
          services.requireService(ProfilesControllerType::class.java)
        val bookmarksService =
          services.requireService(ReaderBookmarkServiceType::class.java)
        val analyticsService =
          services.requireService(AnalyticsType::class.java)

        Reader2ViewModel(
          this.applicationInfo,
          this.parameters,
          profilesController,
          bookmarksService,
          analyticsService,
          this.readerModel
        ) as T
      }
      else -> {
        throw IllegalArgumentException("Cannot instantiate a value of type $modelClass")
      }
    }
  }
}
