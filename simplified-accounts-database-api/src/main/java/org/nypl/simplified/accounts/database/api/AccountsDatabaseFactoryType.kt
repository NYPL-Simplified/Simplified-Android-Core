package org.nypl.simplified.accounts.database.api

import android.content.Context
import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import java.io.File

/**
 * The type of factories that provide account databases.
 */

interface AccountsDatabaseFactoryType {

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param bookDatabases A provider of book databases
   * @param accountProviders The available account providers
   * @param bookFormatSupport The book format support information
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param directory The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    bookDatabases: BookDatabaseFactoryType,
    bookFormatSupport: BookFormatSupportType,
    context: Context,
    directory: File
  ): AccountsDatabaseType

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountEvents An observable that will be used to publish account events
   * @param accountProviders The available account providers
   * @param accountAuthenticationCredentialsStore The credentials store
   * @param bookFormatSupport The book format support information
   * @param directory The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    accountAuthenticationCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    bookFormatSupport: BookFormatSupportType,
    context: Context,
    directory: File
  ): AccountsDatabaseType
}
