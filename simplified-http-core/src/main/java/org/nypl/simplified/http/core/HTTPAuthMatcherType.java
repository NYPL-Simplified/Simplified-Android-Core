package org.nypl.simplified.http.core;

public interface HTTPAuthMatcherType<A, E extends Exception>
{
  A onAuthBasic(
    HTTPAuthBasic b)
    throws E;
}
