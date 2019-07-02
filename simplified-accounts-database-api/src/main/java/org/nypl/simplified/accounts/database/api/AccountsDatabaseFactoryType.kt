package org.nypl.simplified.accounts.database.api

import android.content.Context
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryType
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import org.nypl.simplified.observable.ObservableType
import java.io.File

/**
 * The type of factories that provide account databases.
 */

interface AccountsDatabaseFactoryType {

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param bookDatabases    A provider of book databases
   * @param accountProviders The available account providers
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param accountProviderResolutionListener A listener that will receive the results of account resolution operations
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    bookDatabases: BookDatabaseFactoryType,
    accountProviders: AccountProviderDescriptionRegistryType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountProviderResolutionListener: AccountProviderResolutionListenerType,
    directory: File): AccountsDatabaseType

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param accountProviders The available account providers
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param accountProviderResolutionListener A listener that will receive the results of account resolution operations
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    accountProviders: AccountProviderDescriptionRegistryType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountProviderResolutionListener: AccountProviderResolutionListenerType,
    directory: File): AccountsDatabaseType
}
