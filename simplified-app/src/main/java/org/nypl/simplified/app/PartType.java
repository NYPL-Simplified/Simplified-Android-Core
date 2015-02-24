package org.nypl.simplified.app;

public interface PartType
{
  void partBegin();

  <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E;
}
