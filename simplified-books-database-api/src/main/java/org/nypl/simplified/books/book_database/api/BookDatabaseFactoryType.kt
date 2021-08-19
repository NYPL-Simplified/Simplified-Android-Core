package org.nypl.simplified.books.book_database.api

import android.content.Context
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import java.io.File

/**
 * A factory for producing book databases.
 */

interface BookDatabaseFactoryType {

  /**
   * Open a database, creating a new one if the database does not exist.
   */

  @Throws(BookDatabaseException::class)
  fun openDatabase(
    context: Context,
    parser: OPDSJSONParserType,
    serializer: OPDSJSONSerializerType,
    formats: BookFormatSupportType,
    owner: AccountID,
    directory: File
  ): BookDatabaseType

  /**
   * Open a database, creating a new one if the database does not exist.
   */

  @Throws(BookDatabaseException::class)
  fun openDatabase(
    context: Context,
    formats: BookFormatSupportType,
    owner: AccountID,
    directory: File
  ): BookDatabaseType
}
