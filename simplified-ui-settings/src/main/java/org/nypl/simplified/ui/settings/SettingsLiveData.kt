package org.nypl.simplified.ui.settings

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileDeletionEvent.ProfileDeletionFailed
import org.nypl.simplified.profiles.api.ProfileDeletionEvent.ProfileDeletionSucceeded
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfileUpdated.Failed
import org.nypl.simplified.profiles.api.ProfileUpdated.Succeeded
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.io.File
import java.nio.file.Files

/** stub */

class ProfileEventsLiveData(
  private val profilesController: ProfilesControllerType
) : LiveData<ProfileEvent>() {
  private var subscription: Disposable? = null

  override fun onActive() {
    super.onActive()

    this.subscription =
      this.profilesController
        .profileEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { newEvent ->
          this.value = newEvent
        }
  }

  override fun onInactive() {
    super.onInactive()
    this.subscription?.dispose()
    this.value = null
  }
}

/** stub */

class ProfileLiveData(
  private val profilesController: ProfilesControllerType,
  private val profile: ProfileReadableType
) : LiveData<ProfileReadableType?>() {

  private var subscription: Disposable? = null

  init {
    this.value = this.profile
  }

  override fun onActive() {
    super.onActive()

    this.subscription =
      this.profilesController
        .profileEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onNewProfileEvent)
  }

  override fun onInactive() {
    super.onInactive()
    this.subscription?.dispose()
    this.value = null
  }

  @MainThread
  private fun onProfileUpdated(event: ProfileUpdated) {
    if (event.profileID != this.profile.id) return

    return when (event) {
      is Succeeded -> {
        this.value = this.profilesController.profiles()[this.profile.id]
      }
      is Failed -> {
        // Nothing to do
      }
    }
  }

  @MainThread
  private fun onProfileDeleted(event: ProfileDeletionEvent) {
    if (event.profileID != this.profile.id) return

    return when (event) {
      is ProfileDeletionSucceeded -> {
        this.value = null
      }
      is ProfileDeletionFailed -> {
        // Nothing to do
      }
    }
  }

  @MainThread
  private fun onNewProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileUpdated -> this.onProfileUpdated(event)
      is ProfileDeletionEvent -> this.onProfileDeleted(event)
    }
  }
}

/**
 * [FileSizeLiveData] provides observers with the size, in bytes, of [file].
 *
 * The size is calculated lazily, on a worker thread, when the first active observer
 * is attached.
 *
 * You can force a refresh by invoking [refresh].
 */

class FileSizeLiveData(
  private val file: File
) : LiveData<Long>() {

  var subscription: Disposable? = null

  override fun onActive() {
    super.onActive()
    this.refresh()
  }

  override fun onInactive() {
    super.onInactive()
    this.subscription?.dispose()
    this.value = null
  }

  fun refresh() {
    this.subscription = Single.just(file)
      .subscribeOn(Schedulers.io())
      .map { root ->
        // Walk the filesystem and calculate the total size
        root.walk().map { it.size() }.sum()
      }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { size ->
        this.value = size
      }
  }

  private fun File.size(): Long {
    return if (VERSION.SDK_INT < VERSION_CODES.O) {
      this.length()
    } else {
      Files.size(this.toPath())
    }
  }
}
