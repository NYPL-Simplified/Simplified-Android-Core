package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfilePreferencesChanged
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.Callable

class ProfilePreferencesUpdateTask(
  private val events: Subject<ProfileEvent>,
  private val profile: ProfileType,
  private val preferences: ProfilePreferences
) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(ProfilePreferencesUpdateTask::class.java)

  @Throws(Exception::class)
  override fun call() {
    return try {
      val oldPrefs = this.profile.preferences()
      this.profile.preferencesUpdate(this.preferences)
      this.events.onNext(
        ProfilePreferencesChanged.builder()
          .setChangedReaderPreferences(oldPrefs.readerPreferences() != this.preferences.readerPreferences())
          .build())
    } catch (e: IOException) {
      logger.error("could not update preferences: ", e)
    }
  }
}
