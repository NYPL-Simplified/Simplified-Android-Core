package org.nypl.simplified.books.book_database;

import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;

import java.io.File;

public final class BookDatabases implements BookDatabaseFactoryType {

  private static final BookDatabases INSTANCE = new BookDatabases();

  public static BookDatabases get() {
    return INSTANCE;
  }

  private BookDatabases() {

  }

  @Override
  public BookDatabaseType openDatabase(
      final OPDSJSONParserType parser,
      final OPDSJSONSerializerType serializer,
      final AccountID owner,
      final File directory) throws BookDatabaseException {

    return BookDatabase.open(parser, serializer, owner, directory);
  }

  @Override
  public BookDatabaseType openDatabase(
      final AccountID owner,
      final File directory) throws BookDatabaseException {

    return BookDatabase.open(
        OPDSJSONParser.newParser(), OPDSJSONSerializer.newSerializer(), owner, directory);
  }
}
