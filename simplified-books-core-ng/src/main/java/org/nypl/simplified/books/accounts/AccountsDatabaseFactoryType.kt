package org.nypl.simplified.books.accounts

import android.content.Context
import org.nypl.simplified.books.book_database.BookDatabaseFactoryType

import java.io.File

interface AccountsDatabaseFactoryType {

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param bookDatabases    A provider of book databases
   * @param accountProviders The available account providers
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    bookDatabases: BookDatabaseFactoryType,
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param accountProviders The available account providers
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(AccountsDatabaseException::class)
  fun openDatabase(
    context: Context,
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType
}
