package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP.createAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginErrorData
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
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOKType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.patron.api.PatronDRM
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryCancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
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
  private val http: HTTPType,
  private val loginStrings: AccountLoginStringResourcesType,
  private val patronParsers: PatronUserProfileParsersType,
  private val profile: ProfileReadableType,
  private val request: ProfileAccountLoginRequest
) : Callable<TaskResult<AccountLoginErrorData, Unit>> {

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

  private val steps: TaskRecorderType<AccountLoginErrorData> =
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

  private fun errorDetailsFor(
    message: String,
    errorCode: String,
    exception: Throwable? = null,
    attributes: Map<String, String> = mapOf(),
    problemReport: HTTPProblemReport? = null
  ): AccountLoginErrorData {
    return AccountLoginErrorData(
      attributes = attributes,
      errorCode = errorCode,
      exception = exception,
      message = message,
      problemReport = problemReport
    )
  }

  private fun errorDetailsForUnexpectedException(
    exception: Throwable,
    attributes: Map<String, String> = mapOf()
  ): AccountLoginErrorData {
    return this.errorDetailsFor(
      attributes = attributes,
      errorCode = "unexpectedException",
      exception = exception,
      message = exception.message ?: exception.javaClass.name
    )
  }

  private fun errorDetailsForDRM(
    message: String,
    drmSystem: String,
    errorCode: String,
    exception: Throwable,
    attributes: Map<String, String> = mapOf()
  ): AccountLoginErrorData {
    return this.errorDetailsFor(
      attributes = attributes,
      errorCode = "$drmSystem: $errorCode",
      exception = exception,
      message = message
    )
  }

  private fun run(): TaskResult<AccountLoginErrorData, Unit> {
    return try {
      if (!this.updateLoggingInState(
          this.steps.beginNewStep(this.loginStrings.loginCheckAuthRequired)
        )
      ) {
        return this.steps.finishSuccess(Unit)
      }

      if (!this.validateRequest()) {
        this.debug("account does not support the given authentication")
        val details = errorDetailsFor(this.loginStrings.loginAuthNotRequired, "unsupported")
        this.steps.currentStepFailed(details.message, details)
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
      }
    } catch (e: Throwable) {
      this.logger.error("error during login process: ", e)

      this.steps.currentStepFailedAppending(
        message = this.loginStrings.loginUnexpectedException,
        errorValue = this.errorDetailsForUnexpectedException(e),
        exception = e
      )

      val failure = this.steps.finishFailure<Unit>()
      this.account.setLoginState(AccountLoginFailed(failure))
      failure
    }
  }

  private fun runOAuthWithIntermediaryCancel(
    request: OAuthWithIntermediaryCancel
  ): TaskResult<AccountLoginErrorData, Unit> {
    this.steps.beginNewStep("Cancelling login...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.account.setLoginState(AccountNotLoggedIn)
        this.steps.finishSuccess(Unit)
      }

      AccountNotLoggedIn,
      is AccountLoggingIn,
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
  ): TaskResult<AccountLoginErrorData, Unit> {
    this.steps.beginNewStep("Accepting login token...")
    return when (this.account.loginState) {
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.credentials =
          AccountAuthenticationCredentials.OAuthWithIntermediary(
            accessToken = request.token,
            adobeCredentials = null,
            authenticationDescription = this.findCurrentDescription().description
          )

        this.runPatronProfileRequest()
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
  ): TaskResult.Success<AccountLoginErrorData, Unit> {
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
  ): TaskResult.Success<AccountLoginErrorData, Unit> {
    this.credentials =
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        authenticationDescription = request.description.description,
        adobeCredentials = null
      )

    this.runPatronProfileRequest()
    this.runDeviceActivation()
    this.account.setLoginState(AccountLoggedIn(this.credentials))
    return this.steps.finishSuccess(Unit)
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
      is OAuthWithIntermediaryComplete -> {
        return this.account.provider.authentication is OAuthWithIntermediary ||
          (this.account.provider.authenticationAlternatives.any { it is OAuthWithIntermediary })
      }
      is OAuthWithIntermediaryCancel -> {
        return this.account.provider.authentication is OAuthWithIntermediary ||
          (this.account.provider.authenticationAlternatives.any { it is OAuthWithIntermediary })
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

      this.credentials = this.credentials.withAdobePreActivationCredentials(
        adobePreCredentials.copy(postActivationCredentials = postCredentials.first())
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

  private fun handleAdobeDRMConnectorException(ex: Throwable): TaskStep<AccountLoginErrorData> {
    val text = this.loginStrings.loginDeviceActivationFailed(ex)
    return when (ex) {
      is AdobeDRMExtensions.AdobeDRMLoginNoActivationsException -> {
        this.steps.currentStepFailed(
          text,
          this.errorDetailsForDRM(
            message = text,
            drmSystem = "Adobe ACS",
            errorCode = "drmNoAvailableActivations",
            exception = ex
          ),
          ex
        )
      }
      is AdobeDRMExtensions.AdobeDRMLoginConnectorException -> {
        this.steps.currentStepFailed(
          text,
          this.errorDetailsForDRM(
            message = text,
            drmSystem = "Adobe ACS",
            errorCode = ex.errorCode,
            exception = ex
          ),
          ex
        )
      }
      else -> {
        this.steps.currentStepFailed(
          text,
          this.errorDetailsForDRM(
            message = text,
            drmSystem = "Adobe ACS",
            errorCode = "drmUnspecifiedError",
            exception = ex
          ),
          ex
        )
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

    val httpAuthentication =
      createAuthenticatedHTTP(this.credentials)

    val text =
      adobePostActivationCredentials.deviceID.value + "\n"
    val textBytes =
      text.toByteArray(Charset.forName("UTF-8"))

    /*
     * We don't care if this fails.
     */

    this.http.post(
      Option.some(httpAuthentication),
      deviceManagerURI,
      textBytes,
      "vnd.librarysimplified/drm-device-id-list"
    )

    this.steps.currentStepSucceeded(this.loginStrings.loginDeviceActivationPostDeviceManagerDone)
  }

  private fun <T> someOrNull(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }

  /**
   * Execute a patron profile document request. This fetches patron settings from the remote
   * server and attempts to extract useful information such as DRM-related credentials.
   */

  private fun runPatronProfileRequest() {
    this.debug("running patron profile request")

    this.updateLoggingInState(this.steps.beginNewStep(this.loginStrings.loginPatronSettingsRequest))

    val patronSettingsURI = this.account.provider.patronSettingsURI
    if (patronSettingsURI == null) {
      val message = this.loginStrings.loginPatronSettingsRequestNoURI
      this.steps.currentStepFailed(
        message,
        this.errorDetailsFor(message, "noPatronURI")
      )
      throw Exception()
    }

    val httpAuthentication =
      createAuthenticatedHTTP(this.credentials)
    val result =
      this.http.get(Option.some(httpAuthentication), patronSettingsURI, 0L)

    return when (result) {
      is HTTPResultOKType<InputStream> ->
        this.onPatronProfileRequestOK(patronSettingsURI, result)
      is HTTPResultError<InputStream> ->
        this.onPatronProfileRequestHTTPError(patronSettingsURI, result)
      is HTTPResultException<InputStream> ->
        this.onPatronProfileRequestHTTPException(patronSettingsURI, result)
      else ->
        throw UnreachableCodeException()
    }
  }

  /**
   * A patron settings document was received. Parse it and try to extract any required
   * DRM information.
   */

  private fun onPatronProfileRequestOK(
    patronSettingsURI: URI,
    result: HTTPResultOKType<InputStream>
  ) {
    this.debug("requested patron profile successfully")
    return this.patronParsers.createParser(patronSettingsURI, result.value).use { parser ->
      when (val parseResult = parser.parse()) {
        is ParseResult.Success -> {
          this.debug("parsed patron profile successfully")
          parseResult.warnings.forEach(this::logParseWarning)
          parseResult.result.drm.forEach(this::onPatronProfileRequestHandleDRM)
          this.steps.currentStepSucceeded(this.loginStrings.loginPatronSettingsRequestOK)
        }
        is ParseResult.Failure -> {
          this.error("failed to parse patron profile")
          val message =
            this.loginStrings.loginPatronSettingsRequestParseFailed(
              parseResult.errors.map(this::showParseError)
            )
          this.steps.currentStepFailed(
            message = message,
            errorValue = this.errorDetailsFor(message, "parseErrorPatronSettings")
          )
          throw Exception()
        }
      }
    }
  }

  /**
   * Log and convert a parse error to a humanly-readable string.
   */

  private fun showParseError(error: ParseError): String {
    this.error(
      "{}:{}:{}: {}: ",
      error.source,
      error.line,
      error.column,
      error.message,
      error.exception
    )

    return buildString {
      this.append(error.line)
      this.append(':')
      this.append(error.column)
      this.append(": ")
      this.append(error.message)
      val ex = error.exception
      if (ex != null) {
        this.append(ex.message)
        this.append(" (")
        this.append(ex.javaClass.simpleName)
        this.append(")")
      }
    }
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

  @Suppress("UNUSED_PARAMETER")
  private fun onPatronProfileRequestHTTPException(
    patronSettingsURI: URI,
    result: HTTPResultException<InputStream>
  ) {
    val message = this.loginStrings.loginPatronSettingsConnectionFailed
    this.steps.currentStepFailed(
      message = message,
      errorValue = this.errorDetailsFor(message, "connectionFailed"),
      exception = result.error
    )
    throw result.error
  }

  private fun onPatronProfileRequestHTTPError(
    patronSettingsURI: URI,
    result: HTTPResultError<InputStream>
  ) {
    this.error("received http error: {}: {}: {}", patronSettingsURI, result.message, result.status)

    when (result.status) {
      HttpURLConnection.HTTP_UNAUTHORIZED -> {
        val message = this.loginStrings.loginPatronSettingsInvalidCredentials
        this.steps.currentStepFailed(
          message = message,
          errorValue = this.errorDetailsFor(message, "invalidCredentials")
        )
        throw Exception()
      }
      else -> {
        val message = this.loginStrings.loginServerError(result.status, result.message)
        this.steps.currentStepFailed(
          message = message,
          errorValue = this.errorDetailsFor(
            message = message,
            errorCode = "httpError ${result.status} $patronSettingsURI",
            problemReport = this.someOrNull(result.problemReport)
          )
        )
        throw Exception()
      }
    }
  }

  private fun updateLoggingInState(step: TaskStep<AccountLoginErrorData>): Boolean {
    return when (this.request) {
      is Basic,
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
    }
  }

  private fun logParseWarning(warning: ParseWarning) {
    this.warn(
      "{}:{}:{}: {}: ",
      warning.source,
      warning.line,
      warning.column,
      warning.message,
      warning.exception
    )
  }
}
