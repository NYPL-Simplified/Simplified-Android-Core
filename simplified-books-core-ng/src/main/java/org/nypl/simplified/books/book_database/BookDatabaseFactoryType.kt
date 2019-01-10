package org.nypl.simplified.books.book_database

import android.content.Context
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializerType

import java.io.File

interface BookDatabaseFactoryType {

  @Throws(BookDatabaseException::class)
  fun openDatabase(
    context: Context,
    parser: OPDSJSONParserType,
    serializer: OPDSJSONSerializerType,
    owner: AccountID,
    directory: File): BookDatabaseType

  @Throws(BookDatabaseException::class)
  fun openDatabase(
    context: Context,
    owner: AccountID,
    directory: File): BookDatabaseType
}
