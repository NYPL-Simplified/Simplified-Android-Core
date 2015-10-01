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
   * @param id The book ID
   *
   * @return The most recent feed entry for the given book ID, if one has been
   * explicitly published
   *
   * @see #booksRevocationFeedEntryUpdate(FeedEntryType)
   */

  OptionType<FeedEntryType> booksRevocationFeedEntryGet(BookID id);

  /**
   * Broadcast the fact that a feed entry has been published for a book after
   * the book's loan was revoked. The sole reason this exists is because a view
   * may still be open and observing a book during the revocation process, and
   * when that process has completed, the application has no way retrieve the
   * current state of the book. The feed entry broadcast here is used for that
   * view.
   *
   * @param e The feed entry
   */

  void booksRevocationFeedEntryUpdate(
    FeedEntryType e);

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
