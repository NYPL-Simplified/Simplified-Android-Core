package org.nypl.simplified.books.accounts

import android.content.Context
import org.nypl.simplified.books.book_database.BookDatabaseFactoryType
import org.nypl.simplified.books.book_database.BookDatabases
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
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(context, accountEvents, bookDatabases, accountProviders, directory)
  }

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    context: Context,
    accountEvents: ObservableType<AccountEvent>,
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(context, accountEvents, BookDatabases, accountProviders, directory)
  }
}
