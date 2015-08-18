package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

import java.util.Calendar;

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
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E;
}
