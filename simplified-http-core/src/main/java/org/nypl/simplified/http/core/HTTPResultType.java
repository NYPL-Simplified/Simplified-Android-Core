package org.nypl.simplified.http.core;

/**
 * The type of HTTP results.
 *
 * @param <A>
 *          The precise type of result values
 */

public interface HTTPResultType<A>
{
  /**
   * Match a result value
   * 
   * @param e
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <code>E</code>
   */

  <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> e)
    throws E;
}
