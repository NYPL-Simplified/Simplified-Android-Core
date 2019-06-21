package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData.AccountLogoutDRMFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.AccountLogoutTaskResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
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
  private val profile: ProfileReadableType) : Callable<AccountLogoutTaskResult> {

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
    this.steps.beginNewStep(this.logoutStrings.logoutStarted)

    this.credentials =
      when (val state = this.account.loginState()) {
        is AccountLoginState.AccountLoggedIn -> state.credentials
        is AccountLogoutFailed -> state.credentials
        AccountNotLoggedIn,
        is AccountLoginState.AccountLoggingIn,
        is AccountLoginState.AccountLoginFailed,
        is AccountLoggingOut -> {
          this.warn("attempted to log out with account in state {}", state.javaClass.canonicalName)
          this.steps.currentStepSucceeded(this.logoutStrings.logoutNotLoggedIn)
          return AccountLogoutTaskResult(this.steps.finish())
        }
      }

    return try {
      this.updateLoggingOutState()
      this.runDeviceDeactivation()
      this.runBookRegistryClear()
      this.account.setLoginState(AccountNotLoggedIn)
      AccountLogoutTaskResult(this.steps.finish())
    } catch (e: Throwable) {
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

    this.steps.beginNewStep(this.logoutStrings.logoutDeactivatingDeviceAdobe)
    this.updateLoggingOutState()

    val adobeCredentialsOpt = this.credentials.adobeCredentials()
    if (adobeCredentialsOpt is Some<AccountAuthenticationAdobePreActivationCredentials>) {
      this.runDeviceDeactivationAdobe(adobeCredentialsOpt.get())
      return
    }
  }

  private fun runDeviceDeactivationAdobe(
    adobeCredentials: AccountAuthenticationAdobePreActivationCredentials) {
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

    this.credentials =
      this.credentials.toBuilder()
        .setAdobeCredentials(adobeCredentials.copy(postActivationCredentials = null))
        .build()

    this.steps.currentStepSucceeded(this.logoutStrings.logoutDeactivatingDeviceAdobeDeactivated)

    val deviceManagerURI = adobeCredentials.deviceManagerURI
    if (deviceManagerURI != null) {
      this.runDeviceDeactivationAdobeSendDeviceManagerRequest(
        deviceManagerURI, postActivation.deviceID)
    }
  }

  private fun runDeviceDeactivationAdobeSendDeviceManagerRequest(
    deviceManagerURI: URI,
    deviceID: AdobeDeviceID) {
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
          null,
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
      for (book in this.account.bookDatabase().books()) {
        this.bookRegistry.clearFor(book)
      }
    } catch (e: Throwable) {
      this.error("could not clear book registry: ", e)
      this.steps.currentStepFailed(this.logoutStrings.logoutClearingBookRegistryFailed)
    }

    this.steps.beginNewStep(this.logoutStrings.logoutClearingBookDatabase)
    this.updateLoggingOutState()
    try {
      this.account.bookDatabase().delete()
    } catch (e: Throwable) {
      this.error("could not clear book database: ", e)
      this.steps.currentStepFailed(this.logoutStrings.logoutClearingBookDatabaseFailed)
    }
  }

  private fun pickUsableMessage(message: String, e: Throwable): String {
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
