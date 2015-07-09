package org.nypl.simplified.books.core;

/**
 * The type of book status.
 */

public interface BookStatusType
{
  /**
   * @return The status priority; higher priority status updates will replace
   *         lower priority values.
   */

  BookStatusPriorityOrdering getPriority();

  /**
   * @return The unique identifier of the book
   */

  BookID getID();

  /**
   * Match on the type of status.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E;
}
