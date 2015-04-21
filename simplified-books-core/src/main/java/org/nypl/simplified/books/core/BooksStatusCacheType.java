package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of book status caches.
 *
 * The purpose of the cache is to serve the most recent status of a book
 * without having to access the on-disk database (requiring expensive blocking
 * I/O).
 */

public interface BooksStatusCacheType
{
  /**
   * @return A snapshot of the status of the given book.
   */

  OptionType<BookSnapshot> booksSnapshotGet(
    BookID id);

  /**
   * Update the status of the given book.
   */

  void booksSnapshotUpdate(
    BookID id,
    BookSnapshot snap);

  /**
   * Clear the cache.
   */

  void booksStatusClearAll();

  /**
   * @return The most recent status of the given book, if any.
   */

  OptionType<BookStatusType> booksStatusGet(
    BookID id);

  /**
   * Update the status of the given book. The status will be updated
   * unconditionally. This allows books to return to a less "advanced" status
   * (such as back to being simply "loaned" when having previously been in the
   * process of downloading).
   */

  void booksStatusUpdate(
    BookID id,
    BookStatusLoanedType s);

  /**
   * Mark the given book as being loaned. If this update has appeared late and
   * the book is already in a more "advanced" status (such as being in the
   * process of being downloaded), the update is ignored.
   */

  void booksStatusUpdateLoaned(
    BookID id);

  /**
   * Mark the given book as being requested for downloading.
   */

  void booksStatusUpdateRequesting(
    BookID id);
}
