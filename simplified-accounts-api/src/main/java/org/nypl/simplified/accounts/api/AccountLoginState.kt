package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.Presentables
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import java.io.Serializable
import java.net.URI

/**
 * The current state of an account with respect to logging in/out.
 */

sealed class AccountLoginState {

  abstract val credentials: AccountAuthenticationCredentials?

  /**
   * The account is not logged in.
   */

  object AccountNotLoggedIn : AccountLoginState() {
    override val credentials: AccountAuthenticationCredentials?
      get() = null

    override fun toString(): String =
      this.javaClass.simpleName
  }

  /**
   * The account is currently logging in.
   */

  data class AccountLoggingIn(

    /**
     * A humanly-readable status message.
     *
     * @see AccountLoginStringResourcesType
     */

    val status: String) : AccountLoginState() {

    override val credentials: AccountAuthenticationCredentials?
      get() = null
  }

  /**
   * Error data associated with account login failures.
   */

  sealed class AccountLoginErrorData : PresentableErrorType {

    /**
     * Logging in failed because the credentials were incorrect.
     */

    data class AccountLoginCredentialsIncorrect(
      override val message: String) : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A login attempt was made on an account that doesn't support or require logins.
     */

    data class AccountLoginNotRequired(
      override val message: String) : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A connection could not be made to a remote server.
     */

    data class AccountLoginConnectionFailure(
      override val message: String) : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A connection could not be made to a remote server.
     */

    data class AccountLoginServerParseError(
      override val message: String,
      val warnings: List<ParseWarning>,
      val errors: List<ParseError>)
      : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * Logging in failed because the server returned some sort of error.
     */

    data class AccountLoginServerError(
      override val message: String,
      val uri: URI,
      val statusCode: Int,
      val errorMessage: String,
      override val problemReport: HTTPProblemReport?)
      : AccountLoginErrorData(), HTTPHasProblemReportType {

      override val attributes: Map<String, String>
        get() = Presentables.mergeProblemReportOptional(super.attributes, this.problemReport)
    }

    /**
     * A required DRM system is not supported by the application.
     */

    data class AccountLoginDRMNotSupported(
      override val message: String,
      val system: String)
      : AccountLoginErrorData()

    /**
     * A DRM system failed with an (opaque) error code.
     */

    data class AccountLoginDRMFailure(
      override val message: String,
      val errorCode: String)
      : AccountLoginErrorData()

    /**
     * A DRM system failed due to having too many device activations.
     */

    data class AccountLoginDRMTooManyActivations(
      override val message: String)
      : AccountLoginErrorData()

    /**
     * An unexpected exception occurred.
     */

    data class AccountLoginUnexpectedException(
      override val message: String,
      override val exception: Throwable)
      : AccountLoginErrorData()

    /**
     * Logging in failed due to some missing information.
     */

    data class AccountLoginMissingInformation(
      override val message: String)
      : AccountLoginErrorData()
  }

  /**
   * The account failed to log in.
   */

  data class AccountLoginFailed(
    val taskResult: TaskResult.Failure<AccountLoginErrorData, *>)
    : AccountLoginState() {

    override val credentials: AccountAuthenticationCredentials?
      get() = null
  }

  /**
   * The account is currently logged in.
   */

  data class AccountLoggedIn(
    override val credentials: AccountAuthenticationCredentials)
    : AccountLoginState()

  /**
   * The account is currently logging out.
   */

  data class AccountLoggingOut(
    override val credentials: AccountAuthenticationCredentials,

    /**
     * A humanly-readable status message.
     *
     * @see AccountLogoutStringResourcesType
     */

    val status: String)
    : AccountLoginState()

  /**
   * Data associated with failed logout attempts.
   */

  sealed class AccountLogoutErrorData : Serializable {

    /**
     * A DRM system failed with an (opaque) error code.
     */

    data class AccountLogoutDRMFailure(
      val errorCode: String)
      : AccountLogoutErrorData()

    /**
     * An unexpected exception occurred.
     */

    data class AccountLogoutUnexpectedException(
      val exception: Throwable)
      : AccountLogoutErrorData()

  }

  /**
   * The account failed to log out
   */

  data class AccountLogoutFailed(
    val taskResult: TaskResult.Failure<AccountLogoutErrorData, *>,
    override val credentials: AccountAuthenticationCredentials)
    : AccountLoginState()

}
