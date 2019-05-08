package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorCode.ERROR_ACCOUNTS_DATABASE
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorCode.ERROR_GENERAL
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

internal class ProfileAccountLogoutTask(
  private val bookRegistry: BookRegistryType,
  private val profile: ProfileReadableType,
  private val account: AccountType) : Callable<Unit> {

  private val logger =
    LoggerFactory.getLogger(ProfileAccountLogoutTask::class.java)

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}][{}] ${message}", this.profile.id().uuid, this.account.id(), *arguments)

  override fun call() {
    this.account.setLoginState(AccountLoggingOut)
    try {
      val accountBooks =
        this.account.bookDatabase().books()

      try {
        this.debug("deleting book database")
        this.account.bookDatabase().delete()
        this.account.setLoginState(AccountNotLoggedIn)
      } catch (e: BookDatabaseException) {
        this.error("deleting book database: ", e)
      } finally {
        this.debug("clearing books from book registry")
        for (book in accountBooks) {
          this.bookRegistry.clearFor(book)
        }
      }
    } catch (e: AccountsDatabaseException) {
      this.account.setLoginState(AccountLogoutFailed(ERROR_ACCOUNTS_DATABASE, e))
    } catch (e: Exception) {
      this.account.setLoginState(AccountLogoutFailed(ERROR_GENERAL, e))
    }
  }
}
