package org.nypl.simplified.books.accounts;

import org.nypl.simplified.books.book_database.BookDatabaseFactoryType;
import org.nypl.simplified.books.book_database.BookDatabases;

import java.io.File;

public final class AccountsDatabases implements AccountsDatabaseFactoryType {

  private static final AccountsDatabases INSTANCE = new AccountsDatabases();

  public static AccountsDatabases get() {
    return INSTANCE;
  }

  private AccountsDatabases() {

  }

  @Override
  public AccountsDatabaseType openDatabase(
      final BookDatabaseFactoryType book_databases,
      final AccountProviderCollectionType account_providers,
      final File directory)
      throws AccountsDatabaseException {
    return AccountsDatabase.open(book_databases, account_providers, directory);
  }

  @Override
  public AccountsDatabaseType openDatabase(
      final AccountProviderCollectionType account_providers,
      final File directory)
      throws AccountsDatabaseException {
    return AccountsDatabase.open(BookDatabases.get(), account_providers, directory);
  }
}
