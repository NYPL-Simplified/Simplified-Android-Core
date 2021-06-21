package org.nypl.simplified.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType

/**
 * A view model for storing state during login attempts.
 */

class AccountDetailViewModel(
  val accountId: AccountID
) : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val readerBookmarkService =
    services.requireService(ReaderBookmarkServiceType::class.java)

  private val subscriptions =
    CompositeDisposable(
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent)
    )

  private val accountLiveMutable: MutableLiveData<AccountType> =
    MutableLiveData(
      this.profilesController
        .profileCurrent()
        .account(this.accountId)
    )

  private fun onAccountEvent(accountEvent: AccountEvent) {
    return when (accountEvent) {
      is AccountEventUpdated -> {
        if (accountEvent.accountID == this.accountId) {
          this.accountLiveMutable.value = this.account
        } else {
          // Don't care about events for other accounts
        }
      }
      is AccountEventLoginStateChanged ->
        if (accountEvent.accountID == this.accountId) {
          this.accountLiveMutable.value = this.account
        } else {
          // Don't care about events for other accounts
        }
      else -> {
        // Don't care about other events
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    this.subscriptions.clear()
  }

  val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  val accountLive: LiveData<AccountType> =
    this.accountLiveMutable

  val account: AccountType =
    this.accountLive.value!!

    /**
   * Logging in was explicitly requested. This is tracked in order to allow for optionally
   * closing the account fragment on successful logins.
   */

  @Volatile
  var loginExplicitlyRequested: Boolean = false

  /**
   * Enable/disable bookmark syncing.
   */

  fun enableBookmarkSyncing(enabled: Boolean) {
    this.readerBookmarkService.bookmarkSyncEnable(this.accountId, enabled)
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
