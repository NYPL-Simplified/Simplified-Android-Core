package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorCode.ERROR_CREDENTIALS_INCORRECT
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorCode.ERROR_NETWORK_EXCEPTION
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorCode.ERROR_PROFILE_CONFIGURATION
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorCode.ERROR_SERVER_ERROR
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOKType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.concurrent.Callable

internal class ProfileAccountLoginTask(
  private val booksController: BooksControllerType,
  private val http: HTTPType,
  private val profile: ProfileReadableType,
  private val account: AccountType,
  private val credentials: AccountAuthenticationCredentials) : Callable<Unit> {

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLoginTask::class.java)

  override fun call() =
    this.run()

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  private fun run() {
    this.account.setLoginState(AccountLoggingIn)

    val authenticationOpt =
      this.account.provider().authentication()

    if (authenticationOpt.isNone) {
      this.debug("account does not require authentication")
      this.account.setLoginState(AccountLoggedIn(this.credentials))
    }

    return this.runHTTPRequest(
      (authenticationOpt as Some<AccountProviderAuthenticationDescription>).get())
  }

  /**
   * Hit the login URI using the given authenticated HTTP instance.
   */

  private fun runHTTPRequest(auth: AccountProviderAuthenticationDescription) {

    this.debug("hitting login URI: {}", auth.loginURI())

    val httpAuthentication =
      org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP.createAuthenticatedHTTP(this.credentials)
    val result =
      this.http.head(Option.some(httpAuthentication), auth.loginURI())

    return when (result) {
      is HTTPResultOKType<InputStream> -> this.onHTTPOK(this.account, result)
      is HTTPResultError<InputStream> -> this.onHTTPError(result)
      is HTTPResultException<InputStream> -> this.onHTTPException(result)
      else -> throw UnreachableCodeException()
    }
  }

  private fun onHTTPOK(
    account: AccountType,
    result: HTTPResultOKType<InputStream>) {

    this.debug("received http OK: {}", result.message)

    try {
      account.setLoginState(AccountLoggedIn(this.credentials))
    } catch (e: AccountsDatabaseException) {
      account.setLoginState(AccountLoginFailed(ERROR_PROFILE_CONFIGURATION, e))
    }

    this.booksController.booksSync(account)
  }

  private fun onHTTPException(result: HTTPResultException<InputStream>) {
    this.debug("received http exception: {}: ", result.uri, result.error)
    this.account.setLoginState(AccountLoginFailed(ERROR_NETWORK_EXCEPTION, result.error))
  }

  private fun onHTTPError(result: HTTPResultError<InputStream>) {
    this.debug("received http error: {}: {}", result.message, result.status)

    val code = result.status
    return when (code) {
      HttpURLConnection.HTTP_UNAUTHORIZED ->
        this.account.setLoginState(AccountLoginFailed(ERROR_CREDENTIALS_INCORRECT, null))
      else ->
        this.account.setLoginState(AccountLoginFailed(ERROR_SERVER_ERROR, null))
    }
  }
}
