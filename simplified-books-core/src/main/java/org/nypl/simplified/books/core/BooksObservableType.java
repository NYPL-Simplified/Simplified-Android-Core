package org.nypl.simplified.books.core;

import java.util.Observer;

/**
 * Observable interface for state changes.
 */

public interface BooksObservableType
{
  /**
   * Register an observer.
   *
   * @param o The observer
   */

  void booksObservableAddObserver(
    Observer o);

  /**
   * Remove all observers.
   */

  void booksObservableDeleteAllObservers();

  /**
   * Remove an observer.
   *
   * @param o The observer
   */

  void booksObservableDeleteObserver(
    Observer o);

  /**
   * Notify all observers that the book with {@code id} has changed in some
   * manner.
   *
   * @param id The book ID
   */

  void booksObservableNotify(
    BookID id);
}
