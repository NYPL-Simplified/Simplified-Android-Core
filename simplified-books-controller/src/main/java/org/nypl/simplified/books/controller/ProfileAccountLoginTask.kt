package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeVendorID
import org.nypl.drm.core.DRMUnsupportedException
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP.createAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginConnectionFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginCredentialsIncorrect
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginDRMFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginDRMNotSupported
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginNotRequired
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginServerError
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginServerParseError
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
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
import org.nypl.simplified.profiles.controller.api.AccountLoginTaskResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
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
  initialCredentials: AccountAuthenticationCredentials) : Callable<AccountLoginTaskResult> {

  init {
    Preconditions.checkState(
      this.profile.accounts().containsKey(this.account.id),
      "Profile must contain the given account")
  }

  @Volatile
  private var credentials: AccountAuthenticationCredentials =
    initialCredentials

  private var adobeDRM: PatronDRMAdobe? =
    null

  private val steps: TaskRecorderType<AccountLoginErrorData> =
    TaskRecorder.create()

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLoginTask::class.java)

  override fun call() =
    this.run()

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] ${message}", this.profile.id.uuid, this.account.id, *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}][{}] ${message}", this.profile.id.uuid, this.account.id, *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}][{}] ${message}", this.profile.id.uuid, this.account.id, *arguments)

  private fun checkAuthenticationRequired(): AccountProviderAuthenticationDescription? {
    val authentication = this.account.provider.authentication
    return if (authentication == null) {
      this.debug("account does not require authentication")
      this.steps.currentStepFailed(this.loginStrings.loginAuthNotRequired, AccountLoginNotRequired)
      this.account.setLoginState(AccountLoginFailed(this.steps.finish()))
      null
    } else authentication
  }

  private fun run(): AccountLoginTaskResult {
    try {
      this.steps.beginNewStep(this.loginStrings.loginCheckAuthRequired)
      this.updateLoggingInState()

      return when (this.checkAuthenticationRequired()) {
        is AccountProviderAuthenticationDescription -> {
          this.runPatronProfileRequest()
          this.runDeviceActivation()
          this.account.setLoginState(AccountLoggedIn(this.credentials))
          AccountLoginTaskResult(this.steps.finish())
        }
        else -> {
          this.steps.currentStepSucceeded(this.loginStrings.loginAuthNotRequired)
          AccountLoginTaskResult(this.steps.finish())
        }
      }
    } catch (e: Throwable) {
      val step = this.steps.currentStep()!!
      if (step.exception == null) {
        this.steps.currentStepFailed(
          message = pickUsableMessage(step.resolution, e),
          errorValue = step.errorValue,
          exception = e)
      }

      val resultingSteps = this.steps.finish()
      this.account.setLoginState(AccountLoginFailed(resultingSteps))
      return AccountLoginTaskResult(resultingSteps)
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

    this.steps.beginNewStep(this.loginStrings.loginDeviceActivationAdobe)
    this.updateLoggingInState()

    val deviceManagerURI = adobeDRM.deviceManagerURI
    val adobePreCredentials =
      AccountAuthenticationAdobePreActivationCredentials(
        AdobeVendorID(adobeDRM.vendor),
        AccountAuthenticationAdobeClientToken.create(adobeDRM.clientToken),
        deviceManagerURI,
        null)

    val newCredentials =
      this.credentials.toBuilder()
        .setAdobeCredentials(adobePreCredentials)
        .build()

    this.credentials = newCredentials

    val adeptExecutor = this.adeptExecutor
    if (adeptExecutor == null) {
      this.steps.currentStepFailed(
        this.loginStrings.loginDeviceDRMNotSupported,
        AccountLoginDRMNotSupported("Adobe ACS"))
      throw DRMUnsupportedException("Adobe ACS")
    }

    val adeptFuture =
      AdobeDRMExtensions.activateDevice(
        executor = adeptExecutor,
        error = { message -> this.error("{}", message) },
        debug = { message -> this.debug("{}", message) },
        vendorID = adobePreCredentials.vendorID,
        clientToken = adobePreCredentials.clientToken)

    try {
      val postCredentials =
        adeptFuture.get(1L, TimeUnit.MINUTES)

      Preconditions.checkState(
        postCredentials.isNotEmpty(),
        "Must have returned at least one activation")

      val newPostCredentials =
        this.credentials.toBuilder()
          .setAdobeCredentials(adobePreCredentials.copy(postActivationCredentials = postCredentials.first()))
          .build()

      this.credentials = newPostCredentials
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

  private fun handleAdobeDRMConnectorException(ex: Throwable) =
    when (ex) {
      is AdobeDRMExtensions.AdobeDRMLoginNoActivationsException -> {
        this.steps.currentStepFailed(
          this.loginStrings.loginDeviceActivationFailed(ex),
          null,
          ex)
      }
      is AdobeDRMExtensions.AdobeDRMLoginConnectorException -> {
        this.steps.currentStepFailed(
          this.loginStrings.loginDeviceActivationFailed(ex),
          AccountLoginDRMFailure(ex.errorCode),
          ex)
      }
      else -> {
        this.steps.currentStepFailed(
          this.loginStrings.loginDeviceActivationFailed(ex),
          null,
          ex)
      }
    }

  private fun runDeviceActivationAdobeSendDeviceManagerRequest(deviceManagerURI: URI) {
    this.debug("runDeviceActivationAdobeSendDeviceManagerRequest: posting device ID")

    this.steps.beginNewStep(this.loginStrings.loginDeviceActivationPostDeviceManager)
    this.updateLoggingInState()

    Preconditions.checkState(
      this.credentials.adobeCredentials().isSome,
      "Adobe credentials must be present")
    Preconditions.checkState(
      this.credentials.adobePostActivationCredentials().isSome,
      "Adobe post-activation credentials must be present")

    val adobePreCredentials =
      (this.credentials.adobeCredentials() as Some<AccountAuthenticationAdobePreActivationCredentials>).get()
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
      "vnd.librarysimplified/drm-device-id-list")

    this.steps.currentStepSucceeded(this.loginStrings.loginDeviceActivationPostDeviceManagerDone)
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

    this.steps.beginNewStep(this.loginStrings.loginPatronSettingsRequest)
    this.updateLoggingInState()

    val patronSettingsURI = this.account.provider.patronSettingsURI
    if (patronSettingsURI == null) {
      this.steps.currentStepFailed(this.loginStrings.loginPatronSettingsRequestNoURI)
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
    result: HTTPResultOKType<InputStream>) {
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
          this.steps.currentStepFailed(
            message = this.loginStrings.loginPatronSettingsRequestParseFailed(
              parseResult.errors.map(this::showParseError)),
            errorValue = AccountLoginServerParseError(parseResult.warnings, parseResult.errors))
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
      error.exception)

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

  private fun onPatronProfileRequestHTTPException(
    patronSettingsURI: URI,
    result: HTTPResultException<InputStream>) {
    this.steps.currentStepFailed(
      message = this.loginStrings.loginPatronSettingsConnectionFailed,
      errorValue = AccountLoginConnectionFailure,
      exception = result.error)
    throw result.error
  }

  private fun onPatronProfileRequestHTTPError(
    patronSettingsURI: URI,
    result: HTTPResultError<InputStream>) {
    this.error("received http error: {}: {}: {}", patronSettingsURI, result.message, result.status)

    when (result.status) {
      HttpURLConnection.HTTP_UNAUTHORIZED -> {
        this.steps.currentStepFailed(
          message = this.loginStrings.loginPatronSettingsInvalidCredentials,
          errorValue = AccountLoginCredentialsIncorrect)
        throw Exception()
      }
      else -> {
        this.steps.currentStepFailed(
          message = this.loginStrings.loginServerError(result.status, result.message),
          errorValue = AccountLoginServerError(
            uri = patronSettingsURI,
            statusCode = result.status,
            errorMessage = result.message,
            errorReport = this.someOrNull(result.problemReport)))
        throw Exception()
      }
    }
  }

  private fun updateLoggingInState() {
    this.account.setLoginState(AccountLoggingIn(this.steps.currentStep()?.description ?: ""))
  }

  private fun logParseWarning(warning: ParseWarning) {
    this.warn(
      "{}:{}:{}: {}: ",
      warning.source,
      warning.line,
      warning.column,
      warning.message,
      warning.exception)
  }
}
