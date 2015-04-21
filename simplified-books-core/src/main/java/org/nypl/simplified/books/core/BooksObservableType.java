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

  void addObserver(
    Observer o);

  /**
   * Notify all observers of the book status.
   */

  void booksNotifyObserversUnconditionally(
    BookStatusType status);

  /**
   * Remove an observer.
   */

  void deleteObserver(
    Observer o);

  /**
   * Remove all observers.
   */

  void deleteObservers();
}
