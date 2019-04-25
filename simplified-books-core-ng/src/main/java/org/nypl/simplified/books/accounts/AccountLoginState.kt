package org.nypl.simplified.books.accounts

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
  }

  /**
   * The account is currently logging in.
   */

  object AccountLoggingIn : AccountLoginState() {
    override val credentials: AccountAuthenticationCredentials?
      get() = null
  }

  /**
   * The error codes that can be raised
   */

  enum class AccountLoginErrorCode {

    /**
     * A profile or account configuration problem occurred (such as the user not having
     * selected a profile).
     */

    ERROR_PROFILE_CONFIGURATION,

    /**
     * A network problem occurred whilst trying to contact a remote server.
     */

    ERROR_NETWORK_EXCEPTION,

    /**
     * The provided credentials were rejected by the server.
     */

    ERROR_CREDENTIALS_INCORRECT,

    /**
     * The server responded with an error.
     */

    ERROR_SERVER_ERROR,

    /**
     * The specified account does not exist.
     */

    ERROR_ACCOUNT_NONEXISTENT,

    /**
     * A general error code that is not specifically actionable (such as an I/O error
     * or a programming mistake).
     */

    ERROR_GENERAL
  }

  /**
   * The account failed to log in.
   */

  data class AccountLoginFailed(
    val errorCode: AccountLoginErrorCode,
    val exception: Exception?)
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

  object AccountLoggingOut : AccountLoginState() {
    override val credentials: AccountAuthenticationCredentials?
      get() = null
  }

  /**
   * The error codes that can be raised
   */

  enum class AccountLogoutErrorCode {

    /**
     * A profile or account configuration problem occurred (such as the user not having
     * selected a profile).
     */

    ERROR_PROFILE_CONFIGURATION,

    ERROR_ACCOUNTS_DATABASE,

    /**
     * A general error code that is not specifically actionable (such as an I/O error
     * or a programming mistake).
     */

    ERROR_GENERAL
  }

  /**
   * The account is currently logging out.
   */

  data class AccountLogoutFailed(
    val errorCode: AccountLogoutErrorCode,
    val exception: Exception?)
    : AccountLoginState() {
    override val credentials: AccountAuthenticationCredentials?
      get() = null
  }

}
