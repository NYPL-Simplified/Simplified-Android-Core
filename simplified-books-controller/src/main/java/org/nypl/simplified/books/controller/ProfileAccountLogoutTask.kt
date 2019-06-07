package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.AccountLogoutTaskResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * A task that performs a logout for the given account in the given profile.
 */

class ProfileAccountLogoutTask(
  private val accountLogoutStrings: AccountLogoutStringResourcesType,
  private val bookRegistry: BookRegistryType,
  private val profile: ProfileReadableType,
  private val account: AccountType) : Callable<AccountLogoutTaskResult> {

  init {
    Preconditions.checkState(
      this.profile.accounts().containsKey(this.account.id()),
      "Profile must contain the given account")
  }

  private lateinit var credentials: AccountAuthenticationCredentials

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLogoutTask::class.java)

  private val steps =
    TaskRecorder.create<AccountLogoutErrorData>()

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  override fun call(): AccountLogoutTaskResult {
    this.steps.beginNewStep(this.accountLogoutStrings.logoutStarted)

    this.credentials =
      when (val state = this.account.loginState()) {
        is AccountLoginState.AccountLoggedIn -> state.credentials
        AccountNotLoggedIn,
        is AccountLoginState.AccountLoggingIn,
        is AccountLoginState.AccountLoginFailed,
        is AccountLoggingOut,
        is AccountLogoutFailed -> {
          this.warn("attempted to log out with account in state {}", state.javaClass.canonicalName)
          this.steps.currentStepSucceeded(this.accountLogoutStrings.logoutNotLoggedIn)
          return AccountLogoutTaskResult(this.steps.finish())
        }
      }

    return try {
      this.updateLoggingOutState()
      this.runDeviceDeactivation()
      this.runBookRegistryClear()
      this.account.setLoginState(AccountNotLoggedIn)
      AccountLogoutTaskResult(this.steps.finish())
    } catch (e: Exception) {
      val step = this.steps.currentStep()!!
      if (step.exception == null) {
        this.steps.currentStepFailed(
          message = this.pickUsableMessage(step.resolution, e),
          errorValue = step.errorValue,
          exception = e)
      }

      val resultingSteps = this.steps.finish()
      this.account.setLoginState(AccountLogoutFailed(resultingSteps, this.credentials))
      AccountLogoutTaskResult(this.steps.finish())
    }
  }

  private fun runDeviceDeactivation() {
    this.debug("running device deactivation")

    this.steps.beginNewStep(this.accountLogoutStrings.logoutDeactivatingDeviceAdobe)
    this.updateLoggingOutState()

    val adobeCredentialsOpt = this.credentials.adobeCredentials()
    if (adobeCredentialsOpt is Some<AccountAuthenticationAdobePreActivationCredentials>) {
      val adobeCredentials = adobeCredentialsOpt.get()
      val postActivation = adobeCredentials.postActivationCredentials
      if (postActivation != null) {
        this.debug("device is activated, running deactivation")
        this.steps.currentStepSucceeded(
          this.accountLogoutStrings.logoutDeactivatingDeviceAdobeDeactivated)
      } else {
        this.debug("device does not appear to be activated")
        this.steps.currentStepSucceeded(
          this.accountLogoutStrings.logoutDeactivatingDeviceAdobeNotActive)
      }
    }
  }

  private fun updateLoggingOutState() {
    this.account.setLoginState(AccountLoggingOut(
      this.credentials,
      this.steps.currentStep()?.description ?: ""))
  }

  private fun runBookRegistryClear() {
    this.debug("clearing book database and registry")

    this.steps.beginNewStep(this.accountLogoutStrings.logoutClearingBookRegistry)
    this.updateLoggingOutState()
    try {
      for (book in this.account.bookDatabase().books()) {
        this.bookRegistry.clearFor(book)
      }
    } catch (e: Exception) {
      this.error("could not clear book registry: ", e)
      this.steps.currentStepFailed(this.accountLogoutStrings.logoutClearingBookRegistryFailed)
    }

    this.steps.beginNewStep(this.accountLogoutStrings.logoutClearingBookDatabase)
    this.updateLoggingOutState()
    try {
      this.account.bookDatabase().delete()
    } catch (e: Exception) {
      this.error("could not clear book database: ", e)
      this.steps.currentStepFailed(this.accountLogoutStrings.logoutClearingBookDatabaseFailed)
    }
  }

  private fun pickUsableMessage(message: String, e: Exception): String {
    val exMessage = e.message
    return if (message.isEmpty()) {
      if (exMessage != null) {
        exMessage
      } else {
        e.javaClass.simpleName
      }
    } else {
      message
    }
  }
}
