package org.nypl.simplified.accounts.database

import android.content.Context
import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.book_database.BookDatabases
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import java.io.File

/**
 * The default implementation of the {@link AccountsDatabaseFactoryType} interface.
 */

object AccountsDatabases : AccountsDatabaseFactoryType {

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    bookDatabases: BookDatabaseFactoryType,
    context: Context,
    directory: File
  ): AccountsDatabaseType {
    return AccountsDatabase.open(
      context = context,
      accountEvents = accountEvents,
      bookDatabases = bookDatabases,
      accountCredentials = accountAuthenticationCredentialsStore,
      accountProviders = accountProviders,
      directory = directory
    )
  }

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    context: Context,
    directory: File
  ): AccountsDatabaseType {
    return AccountsDatabase.open(
      context = context,
      accountEvents = accountEvents,
      bookDatabases = BookDatabases,
      accountCredentials = accountAuthenticationCredentialsStore,
      accountProviders = accountProviders,
      directory = directory
    )
  }
}
