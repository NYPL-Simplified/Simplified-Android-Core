package org.nypl.simplified.http.core;

/**
 * The type of HTTP  range values.
 */

public interface HTTPRangeType
{
  /**
   * Match on the type of range value.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchRangeType(
    final HTTPRangeMatcherType<A, E> m)
    throws E;
}
