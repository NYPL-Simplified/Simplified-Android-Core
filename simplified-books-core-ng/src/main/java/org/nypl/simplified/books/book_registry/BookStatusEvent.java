package org.nypl.simplified.books.book_registry;

import com.google.auto.value.AutoValue;

import org.nypl.simplified.books.book_database.BookEvent;
import org.nypl.simplified.books.book_database.BookID;

/**
 * The status of a book has changed.
 */

@AutoValue
public abstract class BookStatusEvent extends BookEvent {

  BookStatusEvent() {

  }

  /**
   * The type of status change.
   */

  public enum Type {

    /**
     * The book status changed.
     */

    BOOK_CHANGED,

    /**
     * The book status was removed.
     */

    BOOK_REMOVED
  }

  /**
   * @return The ID of the book in question
   */

  public abstract BookID book();

  /**
   * @return The type of status change
   */

  public abstract Type type();

  /**
   * Create a book status event.
   *
   * @param book The book
   * @param type The type
   * @return The event
   */

  public static BookStatusEvent create(
      final BookID book,
      final Type type) {
    return new AutoValue_BookStatusEvent(book, type);
  }
}
