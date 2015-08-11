package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.net.URI;

/**
 * A result matcher that returns results in the case that the server responds
 * with a non-error code, raises {@link IOException} in the case of the server
 * responding with an error code, or re-raises any exception raised in the
 * process of creating a connection.
 *
 * @param <A> The type of returned values
 */

@SuppressWarnings("boxing") public final class HTTPResultToException<A>
  implements HTTPResultMatcherType<A, HTTPResultOKType<A>, IOException>
{
  private final URI uri;

  /**
   * Construct a matcher.
   *
   * @param in_uri The HTTP URI
   */

  public HTTPResultToException(
    final URI in_uri)
  {
    this.uri = NullCheck.notNull(in_uri);
  }

  @Override public HTTPResultOKType<A> onHTTPError(
    final HTTPResultError<A> e)
    throws IOException
  {
    final String s = NullCheck.notNull(
      String.format(
        "%s: %d: %s", this.uri, e.getStatus(), e.getMessage()));
    throw new IOException(s);
  }

  @Override public HTTPResultOKType<A> onHTTPException(
    final HTTPResultException<A> e)
    throws IOException
  {
    throw new IOException(e.getError());
  }

  @Override public HTTPResultOKType<A> onHTTPOK(
    final HTTPResultOKType<A> e)
    throws IOException
  {
    return e;
  }
}
