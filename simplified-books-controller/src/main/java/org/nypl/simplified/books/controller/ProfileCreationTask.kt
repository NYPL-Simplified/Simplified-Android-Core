package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_GENERAL
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationSucceeded
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import java.util.concurrent.Callable

class ProfileCreationTask(
  private val displayName: String,
  private val profiles: ProfilesDatabaseType,
  private val profileEvents: Subject<ProfileEvent>,
  private val accountProvider: AccountProviderType,
  private val descriptionUpdate: (ProfileDescription) -> ProfileDescription
) : Callable<ProfileCreationEvent> {

  private fun execute(): ProfileCreationEvent {
    if (this.profiles.findProfileWithDisplayName(this.displayName).isSome) {
      return ProfileCreationFailed.of(this.displayName, ERROR_DISPLAY_NAME_ALREADY_USED, Option.none())
    }

    return try {
      val profile = this.profiles.createProfile(this.accountProvider, this.displayName)
      this.descriptionUpdate(profile.description())
      ProfileCreationSucceeded.of(displayName, profile.id)
    } catch (e: Exception) {
      ProfileCreationFailed.of(displayName, ERROR_GENERAL, Option.some(e))
    }
  }

  @Throws(Exception::class)
  override fun call(): ProfileCreationEvent {
    val event = this.execute()
    this.profileEvents.onNext(event)
    return event
  }
}
