package org.nypl.simplified.books.book_database;

import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;

import java.io.File;

public interface BookDatabaseFactoryType {

  BookDatabaseType openDatabase(
      OPDSJSONParserType parser,
      OPDSJSONSerializerType serializer,
      AccountID owner,
      File directory)
      throws BookDatabaseException;

  BookDatabaseType openDatabase(
      AccountID owner,
      File directory)
      throws BookDatabaseException;
}
