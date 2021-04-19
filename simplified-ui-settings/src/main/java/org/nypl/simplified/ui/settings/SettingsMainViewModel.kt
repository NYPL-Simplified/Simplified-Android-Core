package org.nypl.simplified.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

class SettingsMainViewModel(application: Application) : AndroidViewModel(application) {

  private val services: ServiceDirectoryType =
    Services.serviceDirectory()

  private val profilesController =
    services
      .requireService(ProfilesControllerType::class.java)

  val buildConfig =
    services
      .requireService(BuildConfigurationServiceType::class.java)

  val documents =
    services
      .requireService(DocumentStoreType::class.java)

  var showDebugSettings: Boolean
    get() = this.profilesController
      .profileCurrent()
      .preferences()
      .showDebugSettings
    set(value) {
      this.profilesController.profileUpdate { description ->
        description.copy(
          preferences = description.preferences.copy(
            showDebugSettings = value
          )
        )
      }
    }

  val appVersion: String by lazy {
    try {
      val context = this.getApplication<Application>()
      val pkgManager = context.packageManager
      val pkgInfo = pkgManager.getPackageInfo(context.packageName, 0)
      pkgInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
      "Unknown"
    }
  }
}
