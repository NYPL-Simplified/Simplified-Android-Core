package org.nypl.simplified.app;

/**
 * The type of part activities.
 */

public interface PartActivityType
{
  <A, E extends Exception> A matchPartActivity(
    final PartActivityMatcherType<A, E> m)
    throws E;
}
