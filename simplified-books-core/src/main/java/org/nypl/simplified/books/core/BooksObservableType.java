package org.nypl.simplified.books.core;

import java.util.Observer;

/**
 * Observable interface for state changes.
 */

public interface BooksObservableType
{
  void addObserver(
    Observer o);

  void booksNotifyObserversUnconditionally(
    BookStatusType status);

  void deleteObserver(
    Observer o);

  void deleteObservers();
}
