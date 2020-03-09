package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import io.reactivex.subjects.Subject

import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectFailed
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectFailed.ErrorCode
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfilesDatabaseType

import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountSelectionTask(
  private val profiles: ProfilesDatabaseType,
  private val profileEvents: Subject<ProfileEvent>,
  private val accountProvider: URI
) : Callable<ProfileAccountSelectEvent> {

  private fun run(): ProfileAccountSelectEvent {
    return try {
      val profile = this.profiles.currentProfileUnsafe()
      val idThen = profile.accountCurrent().id
      profile.selectAccount(this.accountProvider)
      ProfileAccountSelectEvent.ProfileAccountSelectSucceeded.of(idThen, profile.accountCurrent().id)
    } catch (e: ProfileNoneCurrentException) {
      ProfileAccountSelectFailed.of(ErrorCode.ERROR_PROFILE_NONE_CURRENT, Option.some(e))
    } catch (e: AccountsDatabaseNonexistentException) {
      ProfileAccountSelectFailed.of(ErrorCode.ERROR_ACCOUNT_NONEXISTENT, Option.some(e))
    }
  }

  @Throws(Exception::class)
  override fun call(): ProfileAccountSelectEvent {
    val event = run()
    this.profileEvents.onNext(event)
    return event
  }
}
