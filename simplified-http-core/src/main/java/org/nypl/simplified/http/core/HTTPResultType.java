package org.nypl.simplified.http.core;

public interface HTTPResultType<A>
{
  <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> e)
    throws E;
}
