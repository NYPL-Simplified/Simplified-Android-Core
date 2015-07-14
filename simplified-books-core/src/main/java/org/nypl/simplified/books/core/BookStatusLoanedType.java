package org.nypl.simplified.books.core;

import java.util.Calendar;

import com.io7m.jfunctional.OptionType;

/**
 * The given book is owned/loaned.
 */

public interface BookStatusLoanedType extends BookStatusType
{
  /**
   * @return The expiry date of the loan, if any.
   */

  OptionType<Calendar> getLoanExpiryDate();

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
