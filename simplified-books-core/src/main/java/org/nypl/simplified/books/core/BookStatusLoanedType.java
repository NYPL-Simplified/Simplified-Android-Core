package org.nypl.simplified.books.core;

/**
 * The given book is owned/loaned.
 */

public interface BookStatusLoanedType extends BookStatusType
{
  /**
   * Match on the type of status.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E;
}
