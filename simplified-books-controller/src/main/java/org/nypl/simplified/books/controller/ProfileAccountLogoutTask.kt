package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData.AccountLogoutDRMFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData.AccountLogoutUnexpectedException
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * A task that performs a logout for the given account in the given profile.
 */

class ProfileAccountLogoutTask(
  private val account: AccountType,
  private val adeptExecutor: AdobeAdeptExecutorType?,
  private val bookRegistry: BookRegistryType,
  private val http: HTTPType,
  private val logoutStrings: AccountLogoutStringResourcesType,
  private val profile: ProfileReadableType
) : Callable<TaskResult<AccountLogoutErrorData, Unit>> {

  init {
    Preconditions.checkState(
      this.profile.accounts().containsKey(this.account.id),
      "Profile must contain the given account")
  }

  private lateinit var credentials: AccountAuthenticationCredentials

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLogoutTask::class.java)

  private val steps =
    TaskRecorder.create<AccountLogoutErrorData>()

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  override fun call(): TaskResult<AccountLogoutErrorData, Unit> {
    this.steps.beginNewStep(this.logoutStrings.logoutStarted)

    this.credentials =
      when (val state = this.account.loginState) {
        is AccountLoggedIn -> state.credentials
        is AccountLogoutFailed -> state.credentials
        is AccountNotLoggedIn,
        is AccountLoggingIn,
        is AccountLoginFailed,
        is AccountLoggingInWaitingForExternalAuthentication,
        is AccountLoggingOut -> {
          this.warn("attempted to log out with account in state {}", state.javaClass.canonicalName)
          this.steps.currentStepSucceeded(this.logoutStrings.logoutNotLoggedIn)
          return this.steps.finishSuccess(Unit)
        }
      }

    return try {
      this.updateLoggingOutState()
      this.runDeviceDeactivation()
      this.runBookRegistryClear()
      this.account.setLoginState(AccountNotLoggedIn)
      return this.steps.finishSuccess(Unit)
    } catch (e: Throwable) {
      this.steps.currentStepFailedAppending(
        message = this.logoutStrings.logoutUnexpectedException,
        errorValue = AccountLogoutUnexpectedException(e),
        exception = e)

      val failure = this.steps.finishFailure<Unit>()
      this.account.setLoginState(AccountLogoutFailed(failure, this.credentials))
      failure
    }
  }

  private fun runDeviceDeactivation() {
    this.debug("running device deactivation")

    this.steps.beginNewStep(this.logoutStrings.logoutDeactivatingDeviceAdobe)
    this.updateLoggingOutState()

    val adobeCredentialsMaybe = this.credentials.adobeCredentials
    if (adobeCredentialsMaybe != null) {
      this.runDeviceDeactivationAdobe(adobeCredentialsMaybe)
      return
    }
  }

  private fun runDeviceDeactivationAdobe(
    adobeCredentials: AccountAuthenticationAdobePreActivationCredentials
  ) {
    val postActivation = adobeCredentials.postActivationCredentials

    if (postActivation == null) {
      this.debug("device does not appear to be activated")
      this.steps.currentStepSucceeded(this.logoutStrings.logoutDeactivatingDeviceAdobeNotActive)
      return
    }

    /*
     * If the Adept executor is not provided, it means that this build of the application
     * has no support for Adobe DRM. We don't treat a missing Adept executor as failure case
     * because if support for Adobe DRM is dropped in the future, it would suddenly become
     * impossible for users to "log out" with activated devices.
     */

    val adeptExecutor = this.adeptExecutor
    if (adeptExecutor == null) {
      this.warn("device is activated but DRM is unsupported")
      this.steps.currentStepSucceeded(this.logoutStrings.logoutDeactivatingDeviceAdobeUnsupported)
      return
    }

    this.debug("device is activated and DRM is supported, running deactivation")

    val adeptFuture =
      AdobeDRMExtensions.deactivateDevice(
        executor = adeptExecutor,
        error = { message -> this.error(message) },
        debug = { message -> this.debug(message) },
        vendorID = adobeCredentials.vendorID,
        userID = postActivation.userID,
        clientToken = adobeCredentials.clientToken)

    try {
      adeptFuture.get(1L, TimeUnit.MINUTES)
    } catch (e: ExecutionException) {
      val ex = e.cause!!
      this.logger.error("exception raised waiting for adept future: ", ex)
      this.handleAdobeDRMConnectorException(ex)
      throw ex
    } catch (e: Throwable) {
      this.logger.error("exception raised waiting for adept future: ", e)
      this.handleAdobeDRMConnectorException(e)
      throw e
    }

    this.credentials = this.credentials.withoutAdobePostActivationCredentials()
    this.steps.currentStepSucceeded(this.logoutStrings.logoutDeactivatingDeviceAdobeDeactivated)

    adobeCredentials.deviceManagerURI?.let { uri ->
      this.runDeviceDeactivationAdobeSendDeviceManagerRequest(uri)
    }
  }

  private fun runDeviceDeactivationAdobeSendDeviceManagerRequest(
    deviceManagerURI: URI
  ) {
    this.debug("runDeviceDeactivationAdobeSendDeviceManagerRequest: posting device ID")

    this.steps.beginNewStep(this.logoutStrings.logoutDeviceDeactivationPostDeviceManager)
    this.updateLoggingOutState()

    val httpAuthentication =
      AccountAuthenticatedHTTP.createAuthenticatedHTTP(this.credentials)

    /*
     * We don't care if this fails.
     *
     * XXX: We're not passing the device ID here!
     */

    this.http.delete(
      Option.some(httpAuthentication),
      deviceManagerURI,
      "vnd.librarysimplified/drm-device-id-list")

    this.steps.currentStepSucceeded(this.logoutStrings.logoutDeviceDeactivationPostDeviceManagerFinished)
  }

  private fun handleAdobeDRMConnectorException(ex: Throwable) =
    when (ex) {
      is AdobeDRMExtensions.AdobeDRMLogoutConnectorException -> {
        this.steps.currentStepFailed(
          this.logoutStrings.logoutDeactivatingDeviceAdobeFailed(ex.errorCode, ex),
          AccountLogoutDRMFailure(ex.errorCode),
          ex)
      }
      else -> {
        this.steps.currentStepFailed(
          this.logoutStrings.logoutDeactivatingDeviceAdobeFailed("UNKNOWN", ex),
          AccountLogoutUnexpectedException(ex),
          ex)
      }
    }

  private fun updateLoggingOutState() {
    this.account.setLoginState(AccountLoggingOut(
      this.credentials,
      this.steps.currentStep()?.description ?: ""))
  }

  private fun runBookRegistryClear() {
    this.debug("clearing book database and registry")

    this.steps.beginNewStep(this.logoutStrings.logoutClearingBookRegistry)
    this.updateLoggingOutState()
    try {
      for (book in this.account.bookDatabase.books()) {
        this.bookRegistry.clearFor(book)
      }
    } catch (e: Throwable) {
      this.error("could not clear book registry: ", e)
      this.steps.currentStepFailed(
        this.logoutStrings.logoutClearingBookRegistryFailed, AccountLogoutUnexpectedException(e))
    }

    this.steps.beginNewStep(this.logoutStrings.logoutClearingBookDatabase)
    this.updateLoggingOutState()
    try {
      this.account.bookDatabase.delete()
    } catch (e: Throwable) {
      this.error("could not clear book database: ", e)
      this.steps.currentStepFailed(
        this.logoutStrings.logoutClearingBookDatabaseFailed, AccountLogoutUnexpectedException(e))
    }
  }
}
