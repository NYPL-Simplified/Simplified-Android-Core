package org.nypl.simplified.main

import androidx.lifecycle.ViewModel
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.Basic
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.crashlytics.api.CrashlyticsServiceType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelection.ProfileSelectionCompleted
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * The view model for the main fragment.
 */

class MainFragmentViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(MainFragmentViewModel::class.java)

  private val disposables = mutableListOf<Disposable>()
  private val services = Services.serviceDirectory()
  private val accountProviders by lazy {
    this.services.requireService(AccountProviderRegistryType::class.java)
  }
  private val profilesController by lazy {
    this.services.requireService(ProfilesControllerType::class.java)
  }
  private val crashlytics by lazy {
    services.optionalService(CrashlyticsServiceType::class.java)
  }

  init {
    this.disposables.add(
      profilesController.accountEvents()
        .subscribe(this::onAccountEvent)
    )
    this.disposables.add(
      profilesController.profileEvents()
        .subscribe(this::onProfileEvent)
    )
  }

  val currentProfile: ProfileReadableType
    get() {
      return this.profilesController.profileCurrent()
    }
  val currentAccount: AccountReadableType?
    get() {
      return this.currentProfile.mostRecentAccount() ?: when (this.currentProfile.accounts().size) {
        0 -> throw UnreachableCodeException() // We expect one account to always exist
        1 -> this.currentProfile.accounts().values.first()
        else -> {
          val defaultProvider = this.accountProviders.defaultProvider
          this.currentProfile.accounts().values.first {
            it.provider.id != defaultProvider.id
          }
        }
      }
    }

  override fun onCleared() {
    super.onCleared()

    // Clear any disposables before the view model is destroyed
    disposables.forEach { it.dispose() }
  }

  private fun onAccountLoginStateChanged(event: AccountEventLoginStateChanged) {
    when (val state = event.state) {
      is AccountNotLoggedIn -> {
        if (event.accountID == this.currentAccount?.id) {
          clearCrashlyticsUserId()
        }
      }
      is AccountLoggedIn -> {
        if (event.accountID == this.currentAccount?.id) {
          setCrashlyticsUserId(state.credentials)
        }
      }
      else -> {
        // Ignored
      }
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventLoginStateChanged -> {
        onAccountLoginStateChanged(event)
      }
      else -> {
        // Ignored
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileSelectionCompleted -> {
        // The active profile changed
        val credentials = this.currentAccount?.loginState?.credentials

        if (credentials == null) {
          clearCrashlyticsUserId()
        } else {
          setCrashlyticsUserId(credentials)
        }
      }
      is ProfileUpdated.Succeeded -> {
        // The profile was updated, this might mean the patron changed
        // the current account.
        val credentials = this.currentAccount?.loginState?.credentials

        if (credentials == null) {
          clearCrashlyticsUserId()
        } else {
          setCrashlyticsUserId(credentials)
        }
      }
      else -> {
        // Ignored
      }
    }
  }

  private fun clearCrashlyticsUserId() {
    this.crashlytics?.setUserId("")
  }

  private fun setCrashlyticsUserId(userId: String) {
    this.crashlytics?.let { service ->
      val hashedUserId = md5(userId)
      service.setUserId(hashedUserId)
    }
  }

  private fun setCrashlyticsUserId(credentials: AccountAuthenticationCredentials) {
    when (credentials) {
      is Basic -> {
        setCrashlyticsUserId(credentials.userName.value)
      }
      else -> {
        clearCrashlyticsUserId()
      }
    }
  }

  /** `true` if the history of tabs should be cleared. */

  var clearHistory: Boolean = true
    set(value) {
      logger.debug("clearHistory set to {}", value)
      field = value
    }
}

private fun md5(value: String): String {
  val md = MessageDigest.getInstance("MD5")
  md.update(value.toByteArray())

  val sb = StringBuilder(64)
  md.digest().forEach { bb ->
    sb.append(String.format("%02x", bb))
  }
  return sb.toString()
}
