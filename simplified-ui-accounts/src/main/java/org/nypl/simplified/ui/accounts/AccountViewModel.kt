package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTime
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.documents.EULAType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.threads.NamedThreadPools

/**
 * A view model for storing state during login attempts.
 */

class AccountViewModel(
  private val accountId: AccountID
) : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val documents =
    services.requireService(DocumentStoreType::class.java)

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val backgroundExecutor =
    NamedThreadPools.namedThreadPool(1, "simplified-accounts-io", 19)

  private val subscriptions = CompositeDisposable(
    this.profilesController.accountEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onAccountEvent),
    this.profilesController.profileEvents()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onProfileEvent)
  )

  private fun onAccountEvent(event: AccountEvent) {
    this.accountEvents.onNext(event)
  }

  private fun onProfileEvent(event: ProfileEvent) {
    this.profileEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    this.subscriptions.clear()
    this.backgroundExecutor.shutdown()
  }

  val accountEvents: UnicastWorkSubject<AccountEvent> =
    UnicastWorkSubject.create()

  val profileEvents: UnicastWorkSubject<ProfileEvent> =
    UnicastWorkSubject.create()

  val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  val eula: EULAType? =
    this.documents.eula

  val account =
    this.profilesController
      .profileCurrent()
      .account(this.accountId)

  /**
   * Logging in was explicitly requested. This is tracked in order to allow for optionally
   * closing the account fragment on successful logins.
   */

  @Volatile
  var loginExplicitlyRequested: Boolean = false

  var bookmarkSyncingPermitted: Boolean
    get() =
      this.account.preferences.bookmarkSyncingPermitted
    set(value) {
      this.backgroundExecutor.execute {
        this.account.setPreferences(
          this.account.preferences.copy(bookmarkSyncingPermitted = value)
        )
      }
    }

  var isOver13: Boolean
    get() {
      val profile = this.profilesController.profileCurrent()
      val age = profile.preferences().dateOfBirth
      return if (age != null) {
        age.yearsOld(DateTime.now()) >= 13
      } else {
        false
      }
    }
    set(value) {
      val fakeDateOfBirth =
        this.synthesizeDateOfBirth(if (value) 14 else 0)

      this.profilesController.profileUpdate { description ->
        description.copy(
          preferences = description.preferences.copy(
            dateOfBirth = fakeDateOfBirth
          )
        )
      }
    }

  /**
   * Synthesize a fake date of birth based on the current date and given age in years.
   */

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth =
    ProfileDateOfBirth(
      date = DateTime.now().minusYears(years),
      isSynthesized = true
    )

  fun tryLogin(request: ProfileAccountLoginRequest) {
    this.profilesController.profileAccountLogin(request)
  }

  fun tryLogout(id: AccountID) {
    this.profilesController.profileAccountLogout(id)
  }
}
