package org.nypl.simplified.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.ViewModelProvider.Factory
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.io.File

/** stub */

class ProfileViewModel : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(ProfileViewModel::class.java)
  private val profilesController: ProfilesControllerType

  val profileEvents: LiveData<ProfileEvent>

  /** The current profile. */

  val profile: LiveData<ProfileReadableType?>
  val profileDescription: LiveData<ProfileDescription?>
  val profilePreferences: LiveData<ProfilePreferences?>

  private var lastProfileUpdate: ProfileDescription? = null

  init {
    val services =
      Services.serviceDirectory()
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.profileEvents =
      ProfileEventsLiveData(this.profilesController)
    this.profile =
      ProfileLiveData(
        this.profilesController,
        this.profilesController.profileCurrent()
      )
    this.profileDescription =
      Transformations.map(this.profile) { profile ->
        profile?.description()
      }
    this.profilePreferences =
      Transformations.map(this.profile) { profile ->
        profile?.preferences()
      }
  }

  fun profileUpdate(update: (ProfileDescription) -> ProfileDescription) {
    val profile =
      this.profile.value ?: throw IllegalStateException("The current profile is null!")
    this.lastProfileUpdate =
      update.invoke(this.lastProfileUpdate ?: profile.description())
    this.profilesController.profileUpdateFor(profile.id) {
      this.lastProfileUpdate!!
    }
  }
}

/** stub */

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

  private val logger =
    LoggerFactory.getLogger(SettingsViewModel::class.java)
  private val context =
    application.applicationContext!!

  /** The app version as a human-readable string. */

  val appVersion by lazy {
    try {
      val pkgManager = this.context.packageManager
      val pkgInfo = pkgManager.getPackageInfo(context.packageName, 0)
      "${pkgInfo.versionName} (${pkgInfo.versionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
      "Unknown"
    }
  }

  /** The location of the internal cache. */

  val cacheDir: LiveData<File> by lazy {
    MutableLiveData(this.context.cacheDir)
  }

  /** The size, in bytes, used by the internal cache. */

  val cacheSize: LiveData<Long> by lazy {
    Transformations.switchMap(cacheDir) { file ->
      FileSizeLiveData(file)
    }
  }

  companion object {
    fun getFactory(application: Application): Factory {
      return AndroidViewModelFactory.getInstance(application)
    }
  }
}
