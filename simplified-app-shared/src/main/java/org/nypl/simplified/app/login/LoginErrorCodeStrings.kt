package org.nypl.simplified.app.login

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_ACCOUNT_NONEXISTENT
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_CREDENTIALS_INCORRECT
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_GENERAL
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_NETWORK_EXCEPTION
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_PROFILE_CONFIGURATION
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLoginErrorCode.ERROR_SERVER_ERROR
import org.nypl.simplified.books.accounts.AccountLoginState.AccountLogoutErrorCode

/**
 * Translated strings for login-related error codes.
 */

object LoginErrorCodeStrings {

  /**
   * Obtain a translated string for the given login error code.
   */

  fun stringOfLoginError(
    resources: Resources,
    code: AccountLoginErrorCode): String =
    when (code) {
      ERROR_PROFILE_CONFIGURATION ->
        /// XXX: This is not correct, need a new translation string for network errors
        resources.getString(R.string.settings_login_failed_server)
      ERROR_NETWORK_EXCEPTION ->
        /// XXX: This is not correct, need a new translation string for network errors
        resources.getString(R.string.settings_login_failed_server)
      ERROR_CREDENTIALS_INCORRECT ->
        resources.getString(R.string.settings_login_failed_credentials)
      ERROR_SERVER_ERROR ->
        resources.getString(R.string.settings_login_failed_server)
      ERROR_ACCOUNT_NONEXISTENT ->
        resources.getString(R.string.settings_login_failed_credentials)
      ERROR_GENERAL ->
        resources.getString(R.string.settings_login_failed_server)
    }

  /**
   * Obtain a translated string for the given logout error code.
   */

  fun stringOfLogoutError(
    resources: Resources,
    code: AccountLogoutErrorCode): String =
    when (code) {
      AccountLogoutErrorCode.ERROR_PROFILE_CONFIGURATION ->
        /// XXX: This is not correct, need a new translation string for network errors
        resources.getString(R.string.settings_login_failed_server)
      AccountLogoutErrorCode.ERROR_ACCOUNTS_DATABASE ->
        /// XXX: This is not correct, need a new translation string for network errors
        resources.getString(R.string.settings_login_failed_server)
      AccountLogoutErrorCode.ERROR_GENERAL ->
        resources.getString(R.string.settings_login_failed_server)
    }

}