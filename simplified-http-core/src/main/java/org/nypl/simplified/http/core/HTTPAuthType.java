package org.nypl.simplified.http.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;

/**
 * The type of HTTP auth mechanisms.
 */

public interface HTTPAuthType extends Serializable
{
  /**
   * Match a mechanism value.
   *
   * @param m   The value
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchAuthType(
    final HTTPAuthMatcherType<A, E> m)
    throws E;

  /**
   * Set the connection parameters for the given connection.
   *
   * @param c The connection
   *
   * @throws IOException On I/O errors
   */

  void setConnectionParameters(
    final HttpURLConnection c)
    throws IOException;
}
