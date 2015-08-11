package org.nypl.simplified.http.core;

/**
 * The type of HTTP auth value matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface HTTPAuthMatcherType<A, E extends Exception>
{
  /**
   * Match an auth value.
   *
   * @param b The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onAuthBasic(
    HTTPAuthBasic b)
    throws E;
}
