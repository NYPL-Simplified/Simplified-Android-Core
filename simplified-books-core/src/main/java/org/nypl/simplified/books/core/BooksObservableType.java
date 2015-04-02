package org.nypl.simplified.books.core;

import java.util.Observer;

/**
 * Observable interface for state changes.
 */

public interface BooksObservableType
{
  void addObserver(
    Observer o);

  void deleteObserver(
    Observer o);

  void deleteObservers();

  void booksNotifyObserversUnconditionally(
    BookStatusType status);
}
