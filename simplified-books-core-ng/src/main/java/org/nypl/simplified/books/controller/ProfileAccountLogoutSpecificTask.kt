package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials

import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventLogout
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutSucceeded
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.accounts.AccountsDatabaseException
import org.nypl.simplified.books.book_database.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.observable.ObservableType
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable

import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_ACCOUNTS_DATABASE
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_GENERAL
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.profiles.ProfilesDatabaseType

internal class ProfileAccountLogoutSpecificTask(
  private val profiles: ProfilesDatabaseType,
  private val bookRegistry: BookRegistryType,
  private val accountID: AccountID,
  private val accountEvents: ObservableType<AccountEvent>) : Callable<AccountEventLogout> {
  private val logger = LoggerFactory.getLogger(ProfileAccountLogoutSpecificTask::class.java)

  override fun call(): AccountEventLogout {
    val event = this.run()
    this.accountEvents.send(event)
    return event
  }

  private fun run(): AccountEventLogout {
    try {
      val profile = this.profiles.currentProfileUnsafe()
      val account = profile.account(this.accountID)
      return this.runForAccount(account)
    } catch (e: AccountsDatabaseException) {
      return AccountLogoutFailed.of(ERROR_ACCOUNTS_DATABASE, Option.some(e))
    } catch (e: Exception) {
      return AccountLogoutFailed.of(ERROR_GENERAL, Option.some(e))
    }
  }

  @Throws(AccountsDatabaseException::class)
  private fun runForAccount(account: AccountType): AccountEventLogout {
    this.logger.debug("clearing account credentials")
    account.setCredentials(Option.none<AccountAuthenticationCredentials>())
    val accountBooks = account.bookDatabase().books()
    try {
      this.logger.debug("deleting book database")
      account.bookDatabase().delete()
    } catch (e: BookDatabaseException) {
      this.logger.error("deleting book database: ", e)
    } finally {
      this.logger.debug("clearing books from book registry")
      for (book in accountBooks) {
        this.bookRegistry.clearFor(book)
      }
    }

    this.logger.debug("logged out successfully")
    return AccountLogoutSucceeded.of(account.id())
  }
}
