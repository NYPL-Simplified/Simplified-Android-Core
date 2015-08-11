package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The type of successful HTTP results.
 *
 * @param <A> The type of result values
 */

public final class HTTPResultOK<A> implements HTTPResultOKType<A>
{
  private final long                      content_length;
  private final Map<String, List<String>> headers;
  private final String                    message;
  private final int                       status;
  private final A                         value;

  /**
   * Construct a result.
   *
   * @param in_message        The server message
   * @param in_status         The status code
   * @param in_value          The resulting value
   * @param in_content_length The content length
   * @param in_headers        The headers
   */

  public HTTPResultOK(
    final String in_message,
    final int in_status,
    final A in_value,
    final long in_content_length,
    final Map<String, List<String>> in_headers)
  {
    this.message = NullCheck.notNull(in_message);
    this.status = in_status;
    this.content_length = in_content_length;
    this.value = NullCheck.notNull(in_value);
    this.headers = NullCheck.notNull(in_headers);
  }

  @Override public void close()
    throws IOException
  {
    // Nothing
  }

  @Override public long getContentLength()
  {
    return this.content_length;
  }

  @Override public String getMessage()
  {
    return this.message;
  }

  @Override public Map<String, List<String>> getResponseHeaders()
  {
    return this.headers;
  }

  @Override public int getStatus()
  {
    return this.status;
  }

  @Override public A getValue()
  {
    return this.value;
  }

  @Override public <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> m)
    throws E
  {
    return m.onHTTPOK(this);
  }
}
