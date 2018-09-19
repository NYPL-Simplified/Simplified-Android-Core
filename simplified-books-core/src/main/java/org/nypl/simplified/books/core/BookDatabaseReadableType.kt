package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType

/**
 * A read-only interface to the book database.
 */

interface BookDatabaseReadableType {

  /**
   * @param book The book ID
   *
   * @return A snapshot of the most recently written data for the given book, if
   * the book exists
   */

  fun databaseGetEntrySnapshot(book: BookID): OptionType<BookDatabaseEntrySnapshot>

  /**
   * @return The set of books currently in the database
   */

  fun databaseGetBooks(): Set<BookID>
}
