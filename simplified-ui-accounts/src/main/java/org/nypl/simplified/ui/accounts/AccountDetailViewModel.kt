package org.nypl.simplified.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent.ReaderBookmarkSyncSettingChanged
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableStatus
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * A view model for storing state during login attempts.
 */

class AccountDetailViewModel(
  private val accountId: AccountID,
  private val listener: FragmentListenerType<AccountDetailEvent>
) : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val readerBookmarkService =
    services.requireService(ReaderBookmarkServiceType::class.java)

  /**
   * Logging in was explicitly requested. This is tracked in order to allow for optionally
   * closing the account fragment on successful logins.
   */

  @Volatile
  private var loginExplicitlyRequested: Boolean = false

  private val subscriptions = CompositeDisposable()

  private val accountLiveMutable: MutableLiveData<AccountType> =
    MutableLiveData(
      this.profilesController
        .profileCurrent()
        .account(this.accountId)
    )

  val accountLive: LiveData<AccountType> =
    this.accountLiveMutable

  val account: AccountType =
    this.accountLive.value!!

  /**
   * A live data element that tracks the status of the bookmark syncing switch for the
   * current account.
   */

  private val accountSyncingSwitchStatusMutable: MutableLiveData<ReaderBookmarkSyncEnableStatus> =
    MutableLiveData(this.readerBookmarkService.bookmarkSyncStatus(account.id))

  val accountSyncingSwitchStatus: LiveData<ReaderBookmarkSyncEnableStatus> =
    this.accountSyncingSwitchStatusMutable

  init {
    this.subscriptions.add(
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent)
    )
    this.subscriptions.add(
      this.readerBookmarkService.bookmarkEvents
        .ofType(ReaderBookmarkSyncSettingChanged::class.java)
        .filter { event -> event.accountID == this.accountId }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onBookmarkEvent)
    )
  }

  private fun onBookmarkEvent(event: ReaderBookmarkSyncSettingChanged) {
    this.accountSyncingSwitchStatusMutable.postValue(event.status)
  }

  private fun onAccountEvent(accountEvent: AccountEvent) {
    when (accountEvent) {
      is AccountEventUpdated -> {
        if (accountEvent.accountID == this.accountId) {
          this.handleAccountUpdated(accountEvent)
        }
      }
      is AccountEventLoginStateChanged -> {
        if (accountEvent.accountID == this.accountId) {
          this.handleLoginStateChanged(accountEvent)
        }
      }
    }
  }

  private fun handleAccountUpdated(event: AccountEventUpdated) {
    this.accountLiveMutable.value = this.account

    /*
     * Synthesize a bookmark event so that we fetch up-to-date values if the account
     * logs in or out.
     */

    this.onBookmarkEvent(
      ReaderBookmarkSyncSettingChanged(
        accountID = event.accountID,
        status = this.readerBookmarkService.bookmarkSyncStatus(event.accountID)
      )
    )
  }

  private fun handleLoginStateChanged(event: AccountEventLoginStateChanged) {
    this.accountLiveMutable.value = this.account

    if (this.loginExplicitlyRequested) {
      when (event.state) {
        is AccountLoginState.AccountLoggedIn -> {
          // Scheduling explicit close of account fragment
          this.loginExplicitlyRequested = false
          this.listener.post(AccountDetailEvent.LoginSucceeded)
        }
        is AccountLoginState.AccountLoginFailed -> {
          this.loginExplicitlyRequested = false
        }
        else -> {
          // Doing nothing
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    this.subscriptions.clear()
  }

  val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  /**
   * Logging out was requested. This is tracked in order to allow for
   * clearing the Barcode and PIN fields after the request has completed.
   */

  var pendingLogout: Boolean = false

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
    this.loginExplicitlyRequested = true
    this.profilesController.profileAccountLogin(request)
  }

  fun tryLogout(): FluentFuture<TaskResult<Unit>> {
    this.pendingLogout = true
    return this.profilesController.profileAccountLogout(this.accountId)
  }

  fun openErrorPage(taskSteps: List<TaskStep>) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = sortedMapOf(),
        taskSteps = taskSteps
      )

    this.listener.post(AccountDetailEvent.OpenErrorPage(parameters))
  }
}
