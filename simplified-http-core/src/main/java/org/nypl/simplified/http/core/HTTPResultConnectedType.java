package org.nypl.simplified.http.core;

import java.util.List;
import java.util.Map;

/**
 * The type of results that are obtained by connections that actually reached
 * the server, even if the server rejected them.
 *
 * @param <A> The precise type of result values
 */

public interface HTTPResultConnectedType<A> extends HTTPResultType<A>
{
  /**
   * @return The last modified time, in seconds since the epoch
   */

  long getLastModifiedTime();

  /**
   * @return The size in bytes of the remote file
   */

  long getContentLength();

  /**
   * @return The message returned by the server (such as "Not found")
   */

  String getMessage();

  /**
   * @return The headers returned by the server
   */

  Map<String, List<String>> getResponseHeaders();

  /**
   * @return The status code returned by the server
   */

  int getStatus();
}
