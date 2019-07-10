package org.nypl.simplified.accounts.api

import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.taskrecorder.api.TaskStep
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

  sealed class AccountLoginErrorData {

    /**
     * Logging in failed because the credentials were incorrect.
     */

    object AccountLoginCredentialsIncorrect : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A login attempt was made on an account that doesn't support or require logins.
     */

    object AccountLoginNotRequired : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A connection could not be made to a remote server.
     */

    object AccountLoginConnectionFailure : AccountLoginErrorData() {
      override fun toString(): String =
        this.javaClass.simpleName
    }

    /**
     * A connection could not be made to a remote server.
     */

    data class AccountLoginServerParseError(
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
      val uri: URI,
      val statusCode: Int,
      val errorMessage: String,
      val errorReport: HTTPProblemReport?)
      : AccountLoginErrorData()

    /**
     * A required DRM system is not supported by the application.
     */

    data class AccountLoginDRMNotSupported(
      val system: String)
      : AccountLoginErrorData()

    /**
     * A DRM system failed with an (opaque) error code.
     */

    data class AccountLoginDRMFailure(
      val errorCode: String)
      : AccountLoginErrorData()
  }

  /**
   * The account failed to log in.
   */

  data class AccountLoginFailed(
    val steps: List<TaskStep<AccountLoginErrorData>>)
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

  sealed class AccountLogoutErrorData {

    /**
     * A DRM system failed with an (opaque) error code.
     */

    data class AccountLogoutDRMFailure(
      val errorCode: String)
      : AccountLogoutErrorData()

  }

  /**
   * The account failed to log out
   */

  data class AccountLogoutFailed(
    val steps: List<TaskStep<AccountLogoutErrorData>>,
    override val credentials: AccountAuthenticationCredentials)
    : AccountLoginState()

}
