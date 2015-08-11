package org.nypl.simplified.http.core;

/**
 * The type of HTTP byte range value matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface HTTPRangeMatcherType<A, E extends Exception>
{
  /**
   * Match byte range value.
   *
   * @param r The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onHTTPByteRangeInclusive(
    HTTPByteRangeInclusive r)
    throws E;

  /**
   * Match byte range value.
   *
   * @param r The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onHTTPByteRangeSuffix(
    HTTPByteRangeSuffix r)
    throws E;
}
