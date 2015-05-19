package org.nypl.simplified.http.core;

public interface HTTPRangeMatcherType<A, E extends Exception>
{
  A onHTTPByteRangeInclusive(
    HTTPByteRangeInclusive r)
    throws E;

  A onHTTPByteRangeSuffix(
    HTTPByteRangeSuffix r)
    throws E;
}
