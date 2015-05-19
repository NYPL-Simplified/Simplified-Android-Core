package org.nypl.simplified.http.core;

public interface HTTPRangeType
{
  <A, E extends Exception> A matchRangeType(
    final HTTPRangeMatcherType<A, E> m)
    throws E;
}
