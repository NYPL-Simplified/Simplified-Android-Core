package org.nypl.simplified.books.book_database.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.util.SortedSet
import javax.annotation.concurrent.ThreadSafe

/**
 * The type of book databases.
 *
 * Implementations are required to be safe to use from multiple threads.
 */

@ThreadSafe
interface BookDatabaseType {

  /**
   * @return The account that owns the database
   */

  fun owner(): AccountID

  /**
   * Retrieve a read-only snapshot of the set of books currently in the database. The returned
   * set is immutable and reflects the contents of the database at the time of retrieval. Subsequent
   * changes to the database will *not* be reflected in the returned set.
   *
   * @return A read-only snapshot of the entries available in the database
   */

  fun books(): SortedSet<org.nypl.simplified.books.api.BookID>

  /**
   * Delete the book database.
   *
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun delete()

  /**
   * Create a new, or update an existing, database entry for the given book ID.
   *
   * @param id The book ID
   * @param entry The current OPDS entry for the book
   * @return A database entry
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun createOrUpdate(
    id: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): BookDatabaseEntryType

  /**
   * Find an existing database entry for the given book ID.
   *
   * @param id The book ID
   * @return A database entry
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun entry(id: BookID): BookDatabaseEntryType
}
