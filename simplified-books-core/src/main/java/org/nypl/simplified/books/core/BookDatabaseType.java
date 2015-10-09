package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.ProcedureType;

import java.io.IOException;

/**
 * The type of book databases.
 */

public interface BookDatabaseType extends BookDatabaseReadableType
{
  /**
   * Create the initial empty database. Has no effect if the database already
   * exists.
   *
   * @throws IOException On I/O errors
   */

  void databaseCreate()
    throws IOException;

  /**
   * Destroy the database.
   *
   * @throws IOException On I/O errors
   */

  void databaseDestroy()
    throws IOException;

  /**
   * Create a new book database entry. If an entry already exists, return that
   * without creating a new one.
   *
   * @param book_id The book ID
   *
   * @return The database entry for {@code book_id}
   */

  BookDatabaseEntryType databaseOpenEntryForWriting(BookID book_id);

  /**
   * Open an existing database entry for reading.
   *
   * @param book_id The book ID
   *
   * @return The database entry for {@code book_id}
   *
   * @throws IOException If the entry does not exist
   */

  BookDatabaseEntryReadableType databaseOpenEntryForReading(BookID book_id)
    throws IOException;

  /**
   * Notify the given status cache of the status of all books within the
   * database.
   *
   * @param cache      The status cache
   * @param on_load    A procedure called for each successfully loaded book
   * @param on_failure A procedure called for each failed book
   */

  void databaseNotifyAllBookStatus(
    BooksStatusCacheType cache,
    ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>> on_load,
    ProcedureType<Pair<BookID, Throwable>> on_failure);
}
