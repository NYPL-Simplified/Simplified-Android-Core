package org.nypl.simplified.accounts.database.api

import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.book_database.api.BookDatabaseType

/**
 * The interface exposed by accounts.
 *
 * An account aggregates a set of credentials and a book database.
 * Account are assigned monotonically increasing identifiers by the
 * application, but the identifiers themselves carry no meaning. It is
 * an error to depend on the values of identifiers for any kind of
 * program logic.
 */

interface AccountType : AccountReadableType {

  /**
   * @return The book database owned by this account
   */

  val bookDatabase: BookDatabaseType

  /**
   * Set the current login state for the account.
   *
   * @param state The login state
   * @throws AccountsDatabaseException On database errors
   */

  @Throws(AccountsDatabaseException::class)
  fun setLoginState(state: AccountLoginState)

  /**
   * Update the account preferences.
   *
   * @param preferences The new preferences
   * @throws AccountsDatabaseException On database errors
   */

  @Throws(AccountsDatabaseException::class)
  fun setPreferences(preferences: AccountPreferences)

  /**
   * Update the account provider. Note that only account providers with id fields matching
   * that of the existing provider will be accepted.
   *
   * @see AccountProviderType.id
   * @param accountProvider The new provider
   * @throws AccountsDatabaseException On database errors
   */

  @Throws(AccountsDatabaseException::class)
  fun setAccountProvider(accountProvider: AccountProviderType)
}
