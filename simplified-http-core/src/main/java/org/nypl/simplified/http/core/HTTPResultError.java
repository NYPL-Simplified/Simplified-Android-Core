package org.nypl.simplified.http.core;

import java.util.List;
import java.util.Map;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class HTTPResultError<A> implements HTTPResultConnectedType<A>
{
  private final long                      content_length;
  private final Map<String, List<String>> headers;
  private final String                    message;
  private final int                       status;

  public HTTPResultError(
    final int in_status,
    final String in_message,
    final long in_content_length,
    final Map<String, List<String>> in_headers)
  {
    this.status = in_status;
    this.content_length = in_content_length;
    this.message = NullCheck.notNull(in_message);
    this.headers = NullCheck.notNull(in_headers);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final HTTPResultError<?> other = (HTTPResultError<?>) obj;
    return this.message.equals(other.message)
      && (this.status == other.status)
      && (this.content_length == other.content_length)
      && (this.headers.equals(other.headers));
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

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.message.hashCode();
    result = (prime * result) + this.status;
    result = (prime * result) + Long.valueOf(this.content_length).hashCode();
    return result;
  }

  @Override public <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> m)
    throws E
  {
    return m.onHTTPError(this);
  }
}
