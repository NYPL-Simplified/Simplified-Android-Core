package org.nypl.simplified.http.core;

/**
 * The type of HTTP result value matchers.
 *
 * @param <A> The type of input values
 * @param <B> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface HTTPResultMatcherType<A, B, E extends Exception>
{
  /**
   * Match a result value.
   *
   * @param e The value
   *
   * @return A value of {@code B}
   *
   * @throws E If required
   */

  B onHTTPError(
    final HTTPResultError<A> e)
    throws E;

  /**
   * Match a result value.
   *
   * @param e The value
   *
   * @return A value of {@code B}
   *
   * @throws E If required
   */

  B onHTTPException(
    final HTTPResultException<A> e)
    throws E;

  /**
   * Match a result value.
   *
   * @param e The value
   *
   * @return A value of {@code B}
   *
   * @throws E If required
   */

  B onHTTPOK(
    final HTTPResultOKType<A> e)
    throws E;
}
