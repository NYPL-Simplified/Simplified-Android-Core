package org.nypl.simplified.http.core;

/**
 * The type of HTTP results.
 *
 * @param <A> The precise type of result values
 */

public interface HTTPResultType<A>
{
  /**
   * Match a result value
   *
   * @param e   The matcher
   * @param <B> The type of transformed results
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> e)
    throws E;
}
