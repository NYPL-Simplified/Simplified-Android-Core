package org.nypl.simplified.http.core;

import java.io.IOException;

import com.io7m.jnull.NullCheck;

/**
 * A result matcher that returns results in the case that the server responds
 * with a non-error code, raises {@link IOException} in the case of the server
 * responding with an error code, or re-raises any exception raised in the
 * process of creating a connection.
 *
 * @param <A>
 *          The type of returned values
 */

@SuppressWarnings("boxing") public final class HTTPResultToException<A> implements
  HTTPResultMatcherType<A, HTTPResultOKType<A>, Exception>
{
  public HTTPResultToException()
  {

  }

  @Override public HTTPResultOKType<A> onHTTPError(
    final HTTPResultError<A> e)
    throws Exception
  {
    final String s =
      NullCheck
        .notNull(String.format("%d: %s", e.getStatus(), e.getMessage()));
    throw new IOException(s);
  }

  @Override public HTTPResultOKType<A> onHTTPException(
    final HTTPResultException<A> e)
    throws Exception
  {
    throw e.getError();
  }

  @Override public HTTPResultOKType<A> onHTTPOK(
    final HTTPResultOKType<A> e)
    throws Exception
  {
    return e;
  }
}
