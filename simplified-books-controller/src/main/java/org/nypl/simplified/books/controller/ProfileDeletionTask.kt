package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNonexistentException
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * A task that deletes a profile from the database.
 */

class ProfileDeletionTask(
  private val profiles: ProfilesDatabaseType,
  private val profileEvents: Subject<ProfileEvent>,
  private val profileID: ProfileID
) : Callable<ProfileDeletionEvent> {

  private val logger =
    LoggerFactory.getLogger(ProfileDeletionTask::class.java)

  private fun execute(): ProfileDeletionEvent {
    return try {
      val profile =
        this.profiles.profiles().get(this.profileID)
          ?: throw ProfileNonexistentException("No such profile: $profileID")

      profile.delete()
      ProfileDeletionEvent.ProfileDeletionSucceeded(this.profileID)
    } catch (e: Exception) {
      this.logger.error("failed to delete profile: ", e)
      ProfileDeletionEvent.ProfileDeletionFailed(this.profileID, e)
    }
  }

  override fun call(): ProfileDeletionEvent {
    val event = this.execute()
    this.profileEvents.onNext(event)
    return event
  }
}
