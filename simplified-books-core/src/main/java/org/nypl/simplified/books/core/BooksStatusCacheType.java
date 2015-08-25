package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of book status caches.
 *
 * The purpose of the cache is to serve the most recent status of a book without
 * having to access the on-disk database (requiring expensive blocking I/O).
 */

public interface BooksStatusCacheType extends BooksObservableType
{
  /**
   * @param id The book ID
   *
   * @return A snapshot of the status of the given book.
   */

  OptionType<BookSnapshot> booksSnapshotGet(
    BookID id);

  /**
   * Update the status of the given book.
   *
   * @param id   The book ID
   * @param snap A book status snapshot
   */

  void booksSnapshotUpdate(
    BookID id,
    BookSnapshot snap);

  /**
   * Clear the cache.
   */

  void booksStatusClearAll();

  /**
   * @param id The book ID
   *
   * @return The most recent status of the given book, if any.
   */

  OptionType<BookStatusType> booksStatusGet(
    BookID id);

  /**
   * Update the status of the book referred to by {@code s}.
   *
   * @param s The book status
   */

  void booksStatusUpdate(
    BookStatusType s);

  /**
   * Update the status of the book referred to by {@code s} if the given status
   * is <i>more important</i> than the current status. <i>Importance</i> is
   * essentially defined by a somewhat arbitrary partial order: {@link
   * BookStatusPriorityOrdering}.
   *
   * @param s The book status
   */

  void booksStatusUpdateIfMoreImportant(
    BookStatusType s);

  /**
   * Clear the book status for the given book.
   *
   * @param book_id The book
   */

  void booksStatusClearFor(BookID book_id);
}
