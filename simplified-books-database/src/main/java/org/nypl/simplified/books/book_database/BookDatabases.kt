package org.nypl.simplified.books.book_database

import android.content.Context
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import java.io.File

object BookDatabases : BookDatabaseFactoryType {

  @Throws(BookDatabaseException::class)
  override fun openDatabase(
    context: Context,
    parser: OPDSJSONParserType,
    serializer: OPDSJSONSerializerType,
    formats: BookFormatSupportType,
    owner: AccountID,
    directory: File
  ): BookDatabaseType {
    return BookDatabase.open(
      context = context,
      parser = parser,
      serializer = serializer,
      formats = formats,
      owner = owner,
      directory = directory
    )
  }

  @Throws(BookDatabaseException::class)
  override fun openDatabase(
    context: Context,
    formats: BookFormatSupportType,
    owner: AccountID,
    directory: File
  ): BookDatabaseType {
    return BookDatabase.open(
      context = context,
      parser = OPDSJSONParser.newParser(),
      serializer = OPDSJSONSerializer.newSerializer(),
      formats = formats,
      owner = owner,
      directory = directory
    )
  }
}
