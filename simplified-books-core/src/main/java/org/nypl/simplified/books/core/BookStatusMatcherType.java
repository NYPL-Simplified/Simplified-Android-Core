package org.nypl.simplified.books.core;

/**
 * The type of matchers for status types.
 *
 * @param <A>
 *          The type of returned values
 * @param <E>
 *          The type of raised exceptions
 */

public interface BookStatusMatcherType<A, E extends Exception>
{
  A onBookStatusLoanedType(
    BookStatusLoanedType o)
    throws E;

  A onBookStatusRequestingLoan(
    BookStatusRequestingLoan s)
    throws E;
}
