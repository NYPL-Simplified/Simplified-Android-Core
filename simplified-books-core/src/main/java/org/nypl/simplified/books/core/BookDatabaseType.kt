package org.nypl.simplified.books.core

import com.io7m.jfunctional.Pair
import com.io7m.jfunctional.ProcedureType

import java.io.IOException

/**
 * The type of book databases.
 */

interface BookDatabaseType : BookDatabaseReadableType {

  /**
   * Create the initial empty database. Has no effect if the database already
   * exists.
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  fun databaseCreate()

  /**
   * Destroy the database.
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  fun databaseDestroy()

  /**
   * Create a new book database entry. If an entry already exists, return that
   * without creating a new one.
   *
   * @param bookID The book ID
   *
   * @return The database entry for `bookID`
   */

  fun databaseOpenEntryForWriting(bookID: BookID): BookDatabaseEntryType

  /**
   * Open an existing database entry for reading.
   *
   * @param bookID The book ID
   *
   * @return The database entry for `bookID`
   *
   * @throws IOException If the entry does not exist
   */

  @Throws(IOException::class)
  fun databaseOpenEntryForReading(bookID: BookID): BookDatabaseEntryReadableType

  /**
   * Notify the given status cache of the status of all books within the
   * database.
   *
   * @param cache     The status cache
   * @param onLoad    A procedure called for each successfully loaded book
   * @param onFailure A procedure called for each failed book
   */

  fun databaseNotifyAllBookStatus(
    cache: BooksStatusCacheType,
    onLoad: ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>>,
    onFailure: ProcedureType<Pair<BookID, Throwable>>)
}
