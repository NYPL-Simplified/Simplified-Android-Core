package org.nypl.simplified.books.accounts

import android.content.Context

import org.nypl.simplified.books.book_database.BookDatabaseFactoryType
import org.nypl.simplified.books.book_database.BookDatabases

import java.io.File

object AccountsDatabases : AccountsDatabaseFactoryType {

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    context: Context,
    bookDatabases: BookDatabaseFactoryType,
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(context, bookDatabases, accountProviders, directory)
  }

  @Throws(AccountsDatabaseException::class)
  override fun openDatabase(
    context: Context,
    accountProviders: AccountProviderCollectionType,
    directory: File): AccountsDatabaseType {
    return AccountsDatabase.open(context, BookDatabases, accountProviders, directory)
  }
}
