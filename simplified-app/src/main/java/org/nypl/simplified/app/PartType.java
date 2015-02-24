package org.nypl.simplified.app;

public interface PartType
{
  <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E;

  void partSwitchTo();
}
