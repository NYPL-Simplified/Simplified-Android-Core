package org.nypl.simplified.books.controller

import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes
import org.nypl.simplified.books.controller.api.BookRevokeExceptionNoCredentials
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNonexistentException
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import java.util.concurrent.Callable

/**
 * A base class for BooksController tasks.
 *
 * It is responsible for locating accounts and failing on exceptions.
 */

abstract class AbstractBookTask(
  private val accountID: AccountID,
  private val profileID: ProfileID,
  private val profiles: ProfilesDatabaseType
) : Callable<TaskResult<Unit>> {

  protected class TaskFailedHandled(override val cause: Throwable) : Exception()

  /**
   * A logger associated with the actual class.
   */
  protected abstract val logger: Logger

  /**
   * A task recorder to work with.
   */
  protected abstract val taskRecorder: TaskRecorderType

  /**
   * The actual task code.
   *
   * Throw a TaskFailedHandled when you already have called taskRecorder.currentStepFailed or
   * any other exception if don't have specific error information to provide the taskRecorder with.
   */
  protected abstract fun execute(account: AccountType): TaskResult.Success<Unit>

  /**
   * Will be called before the task fails when an exception is caught.
   */
  protected abstract fun onFailure(result: TaskResult.Failure<Unit>)

  override fun call(): TaskResult<Unit> {
    this.logger.debug("starting task")

    return try {
      val profile = this.findProfile(profileID)
      val account = this.findAccount(accountID, profile)
      val result = this.execute(account)
      this.logger.debug("task succeeded")
      result
    } catch (e: TaskFailedHandled) {
      this.logger.error("task failed with handled exception: ", e.cause)
      val result = this.taskRecorder.finishFailure<Unit>()
      this.onFailure(result)
      result
    } catch (e: Exception) {
      this.logger.error("task failed with unhandled exception: ", e)
      val msg = e.message ?: e.javaClass.name
      this.taskRecorder.currentStepFailedAppending(msg, BorrowErrorCodes.unexpectedException, e)
      val result = this.taskRecorder.finishFailure<Unit>()
      this.onFailure(result)
      result
    }
  }

  /**
   * Locate the given profile.
   */

  private fun findProfile(
    profileID: ProfileID
  ): ProfileReadableType {
    this.taskRecorder.beginNewStep("Locating profile $profileID...")

    val profile = this.profiles.profiles()[profileID]
    return if (profile == null) {
      this.logger.error("failed to find profile: $profileID")
      val exception = ProfileNonexistentException("No such profile $profileID")
      this.taskRecorder.currentStepFailedAppending(
        message = "Failed to find profile.",
        errorCode = BorrowErrorCodes.profileNotFound,
        exception = exception
      )
      throw TaskFailedHandled(exception)
    } else {
      this.taskRecorder.currentStepSucceeded("Located profile.")
      profile
    }
  }

  /**
   * Locate the account in the current profile.
   */

  private fun findAccount(
    accountID: AccountID,
    profile: ProfileReadableType,
  ): AccountType {
    this.taskRecorder.beginNewStep("Locating account $accountID in the profile...")
    this.taskRecorder.addAttribute("Account ID", accountID.toString())

    val account = try {
      profile.account(accountID)
    } catch (e: Exception) {
      this.logger.error("failed to find account: $accountID", e)
      this.taskRecorder.currentStepFailedAppending(
        message = "Failed to find account.",
        errorCode = BorrowErrorCodes.accountsDatabaseException,
        exception = e
      )

      throw TaskFailedHandled(e)
    }

    this.taskRecorder.addAttribute("Account", account.provider.displayName)
    this.taskRecorder.currentStepSucceeded("Located account.")
    return account
  }

  /**
   * If the account requires credentials, create HTTP auth details. If no credentials
   * are provided, throw an exception.
   */

  protected fun createHttpAuthIfRequired(account: AccountType): LSHTTPAuthorizationType? {
    return if (account.requiresCredentials) {
      AccountAuthenticatedHTTP.createAuthorization(this.getRequiredAccountCredentials(account))
    } else {
      null
    }
  }

  /**
   * Assume that account credentials are required and fetch them. If they're not present, fail
   * loudly.
   */

  protected fun getRequiredAccountCredentials(
    account: AccountType
  ): AccountAuthenticationCredentials {
    val loginState = account.loginState
    val credentials = loginState.credentials
    if (credentials != null) {
      return credentials
    } else {
      this.logger.debug("credentials required but none are available")
      val exception = BookRevokeExceptionNoCredentials()
      this.taskRecorder.currentStepFailed("Credentials required, but none are available.", "credentialsRequired", exception)
      throw TaskFailedHandled(exception)
    }
  }
}
