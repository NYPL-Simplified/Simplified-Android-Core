package org.nypl.simplified.http.core;

public interface HTTPResultMatcherType<A, B, E extends Exception>
{
  B onHTTPError(
    final HTTPResultError<A> e)
    throws E;

  B onHTTPException(
    final HTTPResultException<A> e)
    throws E;

  B onHTTPOK(
    final HTTPResultOKType<A> e)
    throws E;
}
