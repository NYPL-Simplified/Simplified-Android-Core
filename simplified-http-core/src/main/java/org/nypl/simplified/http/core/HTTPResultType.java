package org.nypl.simplified.http.core;

import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of HTTP results.
 *
 * @param <A> The precise type of result values
 */

public interface HTTPResultType<A> {
  /**
   * Match a result value
   *
   * @param e   The matcher
   * @param <B> The type of transformed results
   * @param <E> The type of raised exceptions
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  <B, E extends Exception> B matchResult(
      final HTTPResultMatcherType<A, B, E> e)
      throws E;

  /**
   * Match a result value
   *
   * @param on_error     Called on server errors
   * @param on_exception Called on exceptions
   * @param on_ok        Called on successful results
   * @param <B>          The type of transformed results
   * @param <E>          The type of raised exceptions
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  <B, E extends Exception> B match(
      PartialFunctionType<HTTPResultError<A>, B, E> on_error,
      PartialFunctionType<HTTPResultException<A>, B, E> on_exception,
      PartialFunctionType<HTTPResultOKType<A>, B, E> on_ok)
      throws E;
}
