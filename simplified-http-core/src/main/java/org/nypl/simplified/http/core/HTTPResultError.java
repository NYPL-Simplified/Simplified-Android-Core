package org.nypl.simplified.http.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A result value representing a server-side error.
 *
 * @param <A> The type of value
 */

public final class HTTPResultError<A> implements HTTPResultConnectedType<A>
{
  private final long                          last_modified;
  private final long                          content_length;
  private final Map<String, List<String>>     headers;
  private final String                        message;
  private final int                           status;
  private final InputStream                   data;
  private final OptionType<HTTPProblemReport> report;

  /**
   * Construct an error result.
   *
   * @param in_status         The status code
   * @param in_message        The message
   * @param in_content_length The content length
   * @param in_headers        The server headers
   * @param in_last_modified  The last-modified time of the remote data
   * @param in_data           Any data returned by the server
   * @param in_report         An optional problem report
   */

  public HTTPResultError(
    final int in_status,
    final String in_message,
    final long in_content_length,
    final Map<String, List<String>> in_headers,
    final long in_last_modified,
    final InputStream in_data,
    final OptionType<HTTPProblemReport> in_report)
  {
    this.status = in_status;
    this.content_length = in_content_length;
    this.message = NullCheck.notNull(in_message);
    this.headers = NullCheck.notNull(in_headers);
    this.last_modified = in_last_modified;
    this.data = NullCheck.notNull(in_data);
    this.report = NullCheck.notNull(in_report);
  }

  /**
   * @return The server-returned problem report, if any.
   */

  public OptionType<HTTPProblemReport> getProblemReport()
  {
    return this.report;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final HTTPResultError<?> that = (HTTPResultError<?>) o;
    if (this.last_modified != that.last_modified) {
      return false;
    }
    if (this.content_length != that.content_length) {
      return false;
    }
    if (this.getStatus() != that.getStatus()) {
      return false;
    }
    if (!this.headers.equals(that.headers)) {
      return false;
    }
    if (!this.getMessage().equals(that.getMessage())) {
      return false;
    }
    return this.report.equals(that.report);
  }

  @Override public int hashCode()
  {
    int result = (int) (this.last_modified ^ (this.last_modified >>> 32));
    result =
      31 * result + (int) (this.content_length ^ (this.content_length >>> 32));
    result = 31 * result + this.headers.hashCode();
    result = 31 * result + this.getMessage().hashCode();
    result = 31 * result + this.getStatus();
    result = 31 * result + this.report.hashCode();
    return result;
  }

  @Override public long getLastModifiedTime()
  {
    return this.last_modified;
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

  @Override public <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> m)
    throws E
  {
    return m.onHTTPError(this);
  }

  /**
   * @return Any data returned by the server
   */

  public InputStream getData()
  {
    return this.data;
  }
}
