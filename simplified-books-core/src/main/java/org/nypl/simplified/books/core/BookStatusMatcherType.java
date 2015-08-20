package org.nypl.simplified.books.core;

/**
 * The type of matchers for status types.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface BookStatusMatcherType<A, E extends Exception>
{
  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusHoldable(
    BookStatusHoldable s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusHeld(
    BookStatusHeld s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusReserved(
    BookStatusReserved s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusLoanedType(
    BookStatusLoanedType s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusRequestingLoan(
    BookStatusRequestingLoan s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusLoanable(
    BookStatusLoanable s)
    throws E;
}
