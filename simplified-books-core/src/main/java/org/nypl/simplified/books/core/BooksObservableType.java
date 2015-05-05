package org.nypl.simplified.books.core;

import java.util.Observer;

/**
 * Observable interface for state changes.
 */

public interface BooksObservableType
{
  /**
   * Register an observer.
   */

  void booksObservableAddObserver(
    Observer o);

  /**
   * Remove all observers.
   */

  void booksObservableDeleteAllObservers();

  /**
   * Remove an observer.
   */

  void booksObservableDeleteObserver(
    Observer o);

  /**
   * Notify all observers that the book with <tt>id</tt> has changed in some
   * manner.
   */

  void booksObservableNotify(
    BookID id);
}
