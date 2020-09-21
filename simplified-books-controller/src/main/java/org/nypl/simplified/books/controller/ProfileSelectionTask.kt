package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.joda.time.LocalDateTime
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.profiles.api.ProfileAnonymousEnabledException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNonexistentException
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.profiles.api.ProfileSelection.ProfileSelectionInProgress
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class ProfileSelectionTask(
  private val analytics: AnalyticsType,
  private val bookRegistry: BookRegistryType,
  private val events: Subject<ProfileEvent>,
  private val id: ProfileID,
  private val profiles: ProfilesDatabaseType
) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(ProfileSelectionTask::class.java)

  @Throws(ProfileNonexistentException::class, ProfileAnonymousEnabledException::class)
  override fun call() {
    try {
      this.logger.debug("[{}]: profile selection in progress", this.id.uuid)
      if (this.profiles.anonymousProfileEnabled() === ANONYMOUS_PROFILE_ENABLED) {
        this.events.onNext(ProfileSelectionInProgress(this.id))
        return this.loadData()
      }

      this.profiles.setProfileCurrent(this.id)
      this.events.onNext(ProfileSelectionInProgress(this.id))
      return this.loadData()
    } finally {
      this.logger.debug("[{}]: profile selection completed", this.id.uuid)
      this.events.onNext(ProfileSelection.ProfileSelectionCompleted(this.id))
      this.publishAnalyticsEvent()
    }
  }

  private fun publishAnalyticsEvent() {
    val profile = this.profiles.currentProfileUnsafe()
    this.analytics.publishEvent(
      AnalyticsEvent.ProfileLoggedIn(
        timestamp = LocalDateTime.now(),
        profileUUID = profile.id.uuid,
        displayName = profile.displayName,
        birthDate = profile.preferences().dateOfBirth?.show(),
        attributes = profile.attributes().attributes
      )
    )
  }

  private fun loadData() {
    return ProfileDataLoadTask(
      profile = this.profiles.currentProfileUnsafe(),
      bookRegistry = this.bookRegistry
    )
      .run()
  }
}
