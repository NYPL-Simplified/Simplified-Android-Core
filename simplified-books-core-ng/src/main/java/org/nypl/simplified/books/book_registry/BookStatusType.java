package org.nypl.simplified.books.book_registry;

import org.nypl.simplified.books.book_database.BookID;

/**
 * The type of book status.
 */

public interface BookStatusType
{
  /**
   * @return The status priority; higher priority status updates will replace
   * lower priority values.
   */

  BookStatusPriorityOrdering getPriority();

  /**
   * @return The unique identifier of the book
   */

  BookID getID();

  /**
   * Match on the type of status.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E;
}
