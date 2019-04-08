package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventLogout
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_ACCOUNTS_DATABASE
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_GENERAL
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_PROFILE_CONFIGURATION
import org.nypl.simplified.books.accounts.AccountsDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException
import org.nypl.simplified.books.profiles.ProfilesDatabaseType
import org.nypl.simplified.observable.ObservableType
import java.io.IOException
import java.util.concurrent.Callable

internal class ProfileAccountLogoutTask(
  private val profiles: ProfilesDatabaseType,
  private val bookRegistry: BookRegistryType,
  private val accountEvents: ObservableType<AccountEvent>) : Callable<AccountEventLogout> {

  override fun call(): AccountEventLogout =
    this.run()

  private fun run(): AccountEventLogout =
    try {
      val profile = this.profiles.currentProfileUnsafe()
      val account = profile.accountCurrent()
      ProfileAccountLogoutSpecificTask(
        profiles = this.profiles,
        bookRegistry = this.bookRegistry,
        accountID = account.id(),
        accountEvents = this.accountEvents
      ).call()
    } catch (e: ProfileNoneCurrentException) {
      AccountLogoutFailed.of(ERROR_PROFILE_CONFIGURATION, Option.some(e))
    } catch (e: AccountsDatabaseException) {
      AccountLogoutFailed.of(ERROR_ACCOUNTS_DATABASE, Option.some(e))
    } catch (e: IOException) {
      AccountLogoutFailed.of(ERROR_GENERAL, Option.some(e))
    }
}
