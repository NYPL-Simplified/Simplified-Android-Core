package org.nypl.simplified.books.book_database;

import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.util.SortedMap;
import java.util.SortedSet;

/**
 * The type of book databases.
 */

public interface BookDatabaseType {

  /**
   * @return The account that owns the database
   */

  AccountID owner();

  /**
   * Retrieve a read-only snapshot of the set of books currently in the database. The returned
   * set is immutable and reflects the contents of the database at the time of retrieval. Subsequent
   * changes to the database will *not* be reflected in the returned set.
   *
   * @return A read-only snapshot of the entries available in the database
   */

  SortedSet<BookID> books();

  /**
   * Delete the book database.
   *
   * @throws BookDatabaseException On errors
   */

  void delete()
      throws BookDatabaseException;

  /**
   * Create a new, or update an existing, database entry for the given book ID.
   *
   * @param id    The book ID
   * @param entry The current OPDS entry for the book
   * @return A database entry
   * @throws BookDatabaseException On errors
   */

  BookDatabaseEntryType createOrUpdate(
      BookID id,
      OPDSAcquisitionFeedEntry entry)
      throws BookDatabaseException;

  /**
   * Find an existing database entry for the given book ID.
   *
   * @param id The book ID
   * @return A database entry
   * @throws BookDatabaseException On errors
   */

  BookDatabaseEntryType entry(BookID id)
      throws BookDatabaseException;
}
