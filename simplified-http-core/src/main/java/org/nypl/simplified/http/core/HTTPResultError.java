package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;

public final class HTTPResultError<A> implements HTTPResultType<A>
{
  private final String message;
  private final int    status;

  public HTTPResultError(
    final int in_status,
    final String in_message)
  {
    this.status = in_status;
    this.message = NullCheck.notNull(in_message);
  }

  @Override public boolean equals(
    final Object obj)
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
      && (this.status == other.status);
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.message.hashCode();
    result = (prime * result) + this.status;
    return result;
  }

  @Override public <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> m)
    throws E
  {
    return m.onHTTPError(this);
  }
}
