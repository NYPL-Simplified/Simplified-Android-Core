package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of book status caches.
 *
 * The purpose of the cache is to serve the most recent status of a book
 * without having to access the on-disk database (requiring expensive blocking
 * I/O).
 */

public interface BooksStatusCacheType extends BooksObservableType
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
   * Update the status of the book referred to by <tt>s</tt>.
   *
   * @param s
   *          The book status
   */

  void booksStatusUpdate(
    BookStatusType s);

  /**
   * Update the status of the book referred to by <tt>s</tt> if the given
   * status is <i>more important</i> than the current status.
   * <i>Importance</i> is essentially defined by a somewhat arbitrary partial
   * order: {@link BookStatusPriorityOrdering}.
   *
   * @param s
   *          The book status
   */

  void booksStatusUpdateIfMoreImportant(
    BookStatusType s);
}
