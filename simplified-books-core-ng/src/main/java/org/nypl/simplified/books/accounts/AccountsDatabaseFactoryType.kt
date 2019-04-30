package org.nypl.simplified.books.accounts

import android.content.Context
import org.nypl.simplified.books.book_database.BookDatabaseFactoryType
import org.nypl.simplified.observable.ObservableType
import java.io.File

interface AccountsDatabaseFactoryType {

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param bookDatabases    A provider of book databases
   * @param accountProviders The available account providers
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    bookDatabases: BookDatabaseFactoryType,
    accountProviders: AccountProviderCollectionType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    directory: File): AccountsDatabaseType

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param accountProviders The available account providers
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    accountProviders: AccountProviderCollectionType,
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    directory: File): AccountsDatabaseType
}
