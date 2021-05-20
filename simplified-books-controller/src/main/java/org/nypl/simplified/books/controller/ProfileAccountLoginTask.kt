package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.Method.Post
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.OAuthWithIntermediary
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.SAML2_0
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.patron.api.PatronDRM
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryCancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.SAML20Cancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.SAML20Complete
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.SAML20Initiate
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * A task that performs a login for the given account in the given profile.
 */

class ProfileAccountLoginTask(
  private val account: AccountType,
  private val adeptExecutor: AdobeAdeptExecutorType?,
  private val http: LSHTTPClientType,
  private val loginStrings: AccountLoginStringResourcesType,
  private val patronParsers: PatronUserProfileParsersType,
  private val profile: ProfileReadableType,
  private val request: ProfileAccountLoginRequest
) : Callable<TaskResult<Unit>> {

  init {
    Preconditions.checkState(
      this.profile.accounts().containsKey(this.account.id),
      "Profile must contain the given account"
    )
  }

  @Volatile
  private lateinit var credentials: AccountAuthenticationCredentials

  private var adobeDRM: PatronDRMAdobe? =
    null

  private val steps: TaskRecorderType =
    TaskRecorder.create()

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLoginTask::class.java)

  override fun call() =
    this.run()

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}][{}] $message", this.profile.id.uuid, this.account.id, *arguments)

  private fun run(): TaskResult<Unit> {
    return try {
      if (!this.updateLoggingInState(
          this.steps.beginNewStep(this.loginStrings.loginCheckAuthRequired)
        )
      ) {
        return this.steps.finishSuccess(Unit)
      }

      if (!this.validateRequest()) {
        this.debug("account does not support the given authentication")
        this.steps.currentStepFailed(this.loginStrings.loginAuthNotRequired, "loginAuthNotRequired")
        this.account.setLoginState(AccountLoginFailed(this.steps.finishFailure<Unit>()))
        return this.steps.finishFailure()
      }

      this.steps.currentStepSucceeded(this.loginStrings.loginAuthRequired)

      when (this.request) {
        is Basic ->
          this.runBasicLogin(this.request)
        is OAuthWithIntermediaryInitiate ->
          this.runOAuthWithIntermediaryInitiate(this.request)
        is OAuthWithIntermediaryComplete ->
          this.runOAuthWithIntermediaryComplete(this.request)
        is OAuthWithIntermediaryCancel ->
          this.runOAuthWithIntermediaryCancel(this.request)
        is SAML20Initiate ->
          this.runSAML20Initiate(this.request)
        is SAML20Complete ->
          this.runSAML20Complete(this.request)
        is SAML20Cancel ->
          this.runSAML20Cancel(this.request)
      }
    } catch (e: Throwable) {
      this.logger.error("error during login process: ", e)
      this.steps.currentStepFailedAppending(
        this.loginStrings.loginUnexpectedException, "unexpectedException", e
      )
      val failure = this.steps.finishFailure<Unit>()
      this.account.setLoginState(AccountLoginFailed(failure))
      failure
    }
  }

  private fun runSAML20Cancel(
    request: SAML20Cancel
  ): TaskResult<Unit> {
    this.steps.beginNewStep("Cancelling login...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.account.setLoginState(AccountNotLoggedIn)
        this.steps.finishSuccess(Unit)
      }

      AccountNotLoggedIn,
      is AccountLoginFailed,
      is AccountLoggedIn,
      is AccountLoggingOut,
      is AccountLogoutFailed -> {
        this.steps.currentStepSucceeded(
          "Ignored the cancellation attempt because the account wasn't waiting for authentication."
        )
        this.steps.finishSuccess(Unit)
      }
    }
  }

  private fun runSAML20Complete(
    request: SAML20Complete
  ): TaskResult<Unit> {
    this.steps.beginNewStep("Accepting login token...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.credentials =
          AccountAuthenticationCredentials.SAML2_0(
            accessToken = request.accessToken,
            adobeCredentials = null,
            authenticationDescription = this.findCurrentDescription().description,
            patronInfo = request.patronInfo,
            cookies = request.cookies,
            annotationsURI = null
          )

        this.handlePatronUserProfile()
        this.runDeviceActivation()
        this.account.setLoginState(AccountLoggedIn(this.credentials))
        this.steps.finishSuccess(Unit)
      }

      AccountNotLoggedIn,
      is AccountLoginFailed,
      is AccountLoggedIn,
      is AccountLoggingOut,
      is AccountLogoutFailed -> {
        this.steps.currentStepSucceeded(
          "Ignored the authentication token because the account wasn't waiting for one."
        )
        this.steps.finishSuccess(Unit)
      }
    }
  }

  private fun runSAML20Initiate(
    request: SAML20Initiate
  ): TaskResult<Unit> {
    this.account.setLoginState(
      AccountLoggingInWaitingForExternalAuthentication(
        description = request.description,
        status = "Waiting for authentication..."
      )
    )
    return this.steps.finishSuccess(Unit)
  }

  private fun runOAuthWithIntermediaryCancel(
    request: OAuthWithIntermediaryCancel
  ): TaskResult<Unit> {
    this.steps.beginNewStep("Cancelling login...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.account.setLoginState(AccountNotLoggedIn)
        this.steps.finishSuccess(Unit)
      }

      AccountNotLoggedIn,
      is AccountLoginFailed,
      is AccountLoggedIn,
      is AccountLoggingOut,
      is AccountLogoutFailed -> {
        this.steps.currentStepSucceeded(
          "Ignored the cancellation attempt because the account wasn't waiting for authentication."
        )
        this.steps.finishSuccess(Unit)
      }
    }
  }

  private fun runOAuthWithIntermediaryComplete(
    request: OAuthWithIntermediaryComplete
  ): TaskResult<Unit> {
    this.steps.beginNewStep("Accepting login token...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.credentials =
          AccountAuthenticationCredentials.OAuthWithIntermediary(
            accessToken = request.token,
            adobeCredentials = null,
            authenticationDescription = this.findCurrentDescription().description,
            annotationsURI = null
          )

        this.handlePatronUserProfile()
        this.runDeviceActivation()
        this.account.setLoginState(AccountLoggedIn(this.credentials))
        this.steps.finishSuccess(Unit)
      }

      AccountNotLoggedIn,
      is AccountLoginFailed,
      is AccountLoggedIn,
      is AccountLoggingOut,
      is AccountLogoutFailed -> {
        this.steps.currentStepSucceeded(
          "Ignored the authentication token because the account wasn't waiting for one."
        )
        this.steps.finishSuccess(Unit)
      }
    }
  }

  private fun runOAuthWithIntermediaryInitiate(
    request: OAuthWithIntermediaryInitiate
  ): TaskResult.Success<Unit> {
    this.account.setLoginState(
      AccountLoggingInWaitingForExternalAuthentication(
        description = request.description,
        status = "Waiting for authentication..."
      )
    )
    return this.steps.finishSuccess(Unit)
  }

  private fun runBasicLogin(
    request: Basic
  ): TaskResult.Success<Unit> {
    this.credentials =
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        authenticationDescription = request.description.description,
        adobeCredentials = null,
        annotationsURI = null
      )

    this.handlePatronUserProfile()
    this.runDeviceActivation()
    this.account.setLoginState(AccountLoggedIn(this.credentials))
    return this.steps.finishSuccess(Unit)
  }

  private fun handlePatronUserProfile() {
    val patronProfile =
      PatronUserProfiles.runPatronProfileRequest(
        taskRecorder = this.steps,
        patronParsers = this.patronParsers,
        credentials = this.credentials,
        http = this.http,
        account = this.account
      )
    patronProfile.drm.map(this::onPatronProfileRequestHandleDRM)

    /*
     * Copy the annotations link out of the patron profile.
     */

    this.credentials = when (val currentCredentials = this.credentials) {
      is AccountAuthenticationCredentials.Basic ->
        currentCredentials.copy(annotationsURI = patronProfile.annotationsURI)
      is AccountAuthenticationCredentials.OAuthWithIntermediary ->
        currentCredentials.copy(annotationsURI = patronProfile.annotationsURI)
      is AccountAuthenticationCredentials.SAML2_0 ->
        currentCredentials.copy(annotationsURI = patronProfile.annotationsURI)
    }
  }

  private fun validateRequest(): Boolean {
    this.debug("validating login request")

    return when (this.request) {
      is Basic -> {
        (this.account.provider.authentication == this.request.description) ||
          (this.account.provider.authenticationAlternatives.any { it == this.request.description })
      }

      is OAuthWithIntermediaryInitiate -> {
        (this.account.provider.authentication == this.request.description) ||
          (this.account.provider.authenticationAlternatives.any { it == this.request.description })
      }
      is OAuthWithIntermediaryCancel,
      is OAuthWithIntermediaryComplete -> {
        this.account.provider.authentication is OAuthWithIntermediary ||
          (this.account.provider.authenticationAlternatives.any { it is OAuthWithIntermediary })
      }

      is SAML20Initiate -> {
        (this.account.provider.authentication == this.request.description) ||
          (this.account.provider.authenticationAlternatives.any { it == this.request.description })
      }
      is SAML20Cancel,
      is SAML20Complete -> {
        this.account.provider.authentication is SAML2_0 ||
          (this.account.provider.authenticationAlternatives.any { it is SAML2_0 })
      }
    }
  }

  private fun runDeviceActivation() {
    this.debug("running device activation")

    val adobeDRMValues = this.adobeDRM
    if (adobeDRMValues != null) {
      this.runDeviceActivationAdobe(adobeDRMValues)
    }
  }

  private fun runDeviceActivationAdobe(adobeDRM: PatronDRMAdobe) {
    this.debug("runDeviceActivationAdobe: executing")

    this.updateLoggingInState(this.steps.beginNewStep(this.loginStrings.loginDeviceActivationAdobe))

    val deviceManagerURI = adobeDRM.deviceManagerURI
    val adobePreCredentials =
      AccountAuthenticationAdobePreActivationCredentials(
        clientToken = AccountAuthenticationAdobeClientToken.parse(adobeDRM.clientToken),
        deviceManagerURI = deviceManagerURI,
        postActivationCredentials = null,
        vendorID = AdobeVendorID(adobeDRM.vendor)
      )

    this.credentials =
      this.credentials.withAdobePreActivationCredentials(adobePreCredentials)

    /*
     * We can only activate a device if there's a support Adept executor available.
     * We don't treat lack of support as a hard error here.
     */

    val adeptExecutor = this.adeptExecutor
    if (adeptExecutor == null) {
      this.steps.currentStepSucceeded(this.loginStrings.loginDeviceDRMNotSupported)
      return
    }

    val adeptFuture =
      AdobeDRMExtensions.activateDevice(
        executor = adeptExecutor,
        error = { message -> this.error("{}", message) },
        debug = { message -> this.debug("{}", message) },
        vendorID = adobePreCredentials.vendorID,
        clientToken = adobePreCredentials.clientToken
      )

    try {
      val postCredentials =
        adeptFuture.get(1L, TimeUnit.MINUTES)

      Preconditions.checkState(
        postCredentials.isNotEmpty(),
        "Must have returned at least one activation"
      )

      /*
       * Find the newly activated credentials (the one whose user id is not associated with any
       * account). There should only be one, and it should be the last in the list, but this check
       * makes sure.
       */

      val newPostCredentials = postCredentials.last { credentials ->
        this.profile.accounts().values.none { account ->
          account.loginState.credentials?.adobeCredentials?.postActivationCredentials?.userID ==
            credentials.userID
        }
      }

      this.credentials = this.credentials.withAdobePreActivationCredentials(
        adobePreCredentials.copy(postActivationCredentials = newPostCredentials)
      )
      this.steps.currentStepSucceeded(this.loginStrings.loginDeviceActivated)
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

    if (deviceManagerURI != null) {
      this.runDeviceActivationAdobeSendDeviceManagerRequest(deviceManagerURI)
    }
  }

  private fun handleAdobeDRMConnectorException(ex: Throwable): TaskStep {
    val text = this.loginStrings.loginDeviceActivationFailed(ex)
    return when (ex) {
      is AdobeDRMExtensions.AdobeDRMLoginNoActivationsException -> {
        this.steps.currentStepFailed(text, "Adobe ACS: drmNoAvailableActivations", ex)
      }
      is AdobeDRMExtensions.AdobeDRMLoginConnectorException -> {
        this.steps.currentStepFailed(text, "Adobe ACS: ${ex.errorCode}", ex)
      }
      else -> {
        this.steps.currentStepFailed(text, "Adobe ACS: drmUnspecifiedError", ex)
      }
    }
  }

  private fun runDeviceActivationAdobeSendDeviceManagerRequest(deviceManagerURI: URI) {
    this.debug("runDeviceActivationAdobeSendDeviceManagerRequest: posting device ID")

    this.updateLoggingInState(this.steps.beginNewStep(this.loginStrings.loginDeviceActivationPostDeviceManager))

    val adobePreCredentials =
      this.credentials.adobeCredentials

    Preconditions.checkState(
      adobePreCredentials != null,
      "Adobe credentials must be present"
    )
    Preconditions.checkState(
      adobePreCredentials!!.postActivationCredentials != null,
      "Adobe post-activation credentials must be present"
    )

    val adobePostActivationCredentials =
      adobePreCredentials.postActivationCredentials!!
    val text =
      adobePostActivationCredentials.deviceID.value + "\n"
    val textBytes =
      text.toByteArray(Charset.forName("UTF-8"))

    /*
     * We don't care if this fails.
     */

    val post =
      Post(
        body = textBytes,
        contentType = MIMEType("vnd.librarysimplified", "drm-device-id-list", mapOf())
      )

    val request =
      this.http.newRequest(deviceManagerURI)
        .setAuthorization(AccountAuthenticatedHTTP.createAuthorizationIfPresent(this.credentials))
        .setMethod(post)
        .build()

    request.execute()

    this.steps.currentStepSucceeded(this.loginStrings.loginDeviceActivationPostDeviceManagerDone)
  }

  /**
   * Process a DRM item.
   */

  private fun onPatronProfileRequestHandleDRM(drm: PatronDRM) {
    return when (drm) {
      is PatronDRMAdobe -> this.onPatronProfileRequestHandleDRMAdobe(drm)
      else -> {
      }
    }
  }

  private fun onPatronProfileRequestHandleDRMAdobe(drm: PatronDRMAdobe) {
    this.debug("received Adobe DRM client token")
    this.adobeDRM = drm
  }

  private fun updateLoggingInState(step: TaskStep): Boolean {
    return when (this.request) {
      is Basic,
      is SAML20Initiate,
      is OAuthWithIntermediaryInitiate -> {
        this.account.setLoginState(
          AccountLoggingIn(
            status = step.description,
            description = this.findCurrentDescription(),
            cancellable = false
          )
        )
        true
      }

      is SAML20Cancel,
      is SAML20Complete,
      is OAuthWithIntermediaryComplete,
      is OAuthWithIntermediaryCancel -> {
        when (this.account.loginState) {
          is AccountLoggingInWaitingForExternalAuthentication -> {
            this.account.setLoginState(
              AccountLoggingIn(
                status = step.description,
                description = this.findCurrentDescription(),
                cancellable = false
              )
            )
            true
          }

          AccountNotLoggedIn,
          is AccountLoggingIn,
          is AccountLoginFailed,
          is AccountLoggedIn,
          is AccountLoggingOut,
          is AccountLogoutFailed -> {
            this.steps.currentStepSucceeded("Ignored an unexpected completion/cancellation attempt.")
            false
          }
        }
      }
    }
  }

  private class NoCurrentDescription : Exception()

  private fun findCurrentDescription(): AccountProviderAuthenticationDescription {
    return when (this.request) {
      is Basic ->
        this.request.description
      is OAuthWithIntermediaryInitiate ->
        this.request.description
      is OAuthWithIntermediaryCancel ->
        this.request.description
      is OAuthWithIntermediaryComplete ->
        when (val loginState = this.account.loginState) {
          is AccountLoggingIn ->
            loginState.description
          is AccountLoggingInWaitingForExternalAuthentication ->
            loginState.description
          AccountNotLoggedIn,
          is AccountLoginFailed,
          is AccountLoggedIn,
          is AccountLoggingOut,
          is AccountLogoutFailed ->
            throw NoCurrentDescription()
        }
      is SAML20Initiate ->
        this.request.description
      is SAML20Cancel ->
        this.request.description
      is SAML20Complete ->
        when (val loginState = this.account.loginState) {
          is AccountLoggingIn ->
            loginState.description
          is AccountLoggingInWaitingForExternalAuthentication ->
            loginState.description
          AccountNotLoggedIn,
          is AccountLoginFailed,
          is AccountLoggedIn,
          is AccountLoggingOut,
          is AccountLogoutFailed ->
            throw NoCurrentDescription()
        }
    }
  }
}
