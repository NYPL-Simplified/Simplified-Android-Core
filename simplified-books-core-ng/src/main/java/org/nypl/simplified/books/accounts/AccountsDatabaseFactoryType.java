package org.nypl.simplified.books.accounts;

import org.nypl.simplified.books.book_database.BookDatabaseFactoryType;

import java.io.File;

public interface AccountsDatabaseFactoryType {

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param book_databases    A provider of book databases
   * @param account_providers The available account providers
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  AccountsDatabaseType openDatabase(
      final BookDatabaseFactoryType book_databases,
      final AccountProviderCollectionType account_providers,
      final File directory)
      throws AccountsDatabaseException;

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param account_providers The available account providers
   * @param directory         The directory
   * @return A profile database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  AccountsDatabaseType openDatabase(
      final AccountProviderCollectionType account_providers,
      final File directory)
      throws AccountsDatabaseException;
}
