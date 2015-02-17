package org.nypl.simplified.app;

/**
 * The type of part activities.
 */

public interface PartActivityType
{
  /**
   * Match on the type of this part.
   *
   * @param m
   *          A matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <code>E</code>
   * @param <A>
   *          The type of returned values
   * @param <E>
   *          The type of raised exceptions
   */

  <A, E extends Exception> A matchPartActivity(
    final PartActivityMatcherType<A, E> m)
    throws E;
}
