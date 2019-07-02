package org.nypl.simplified.accounts.database

import android.content.Context
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryType
import org.nypl.simplified.books.book_database.BookDatabases
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import org.nypl.simplified.observable.ObservableType
import java.io.File

/**
 * The default implementation of the {@link AccountsDatabaseFactoryType} interface.
 */

object AccountsDatabases : AccountsDatabaseFactoryType {

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    bookDatabases: BookDatabaseFactoryType,
    accountProviders: AccountProviderDescriptionRegistryType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountProviderResolutionListener: AccountProviderResolutionListenerType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(
      context,
      accountEvents,
      bookDatabases,
      accountProviders,
      accountAuthenticationCredentialsStore,
      accountProviderResolutionListener,
      directory)
  }

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    accountProviders: AccountProviderDescriptionRegistryType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountProviderResolutionListener: AccountProviderResolutionListenerType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(
      context,
      accountEvents,
      BookDatabases,
      accountProviders,
      accountAuthenticationCredentialsStore,
      accountProviderResolutionListener,
      directory)
  }
}
