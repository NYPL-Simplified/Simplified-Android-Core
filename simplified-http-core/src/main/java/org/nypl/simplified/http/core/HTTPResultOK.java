package org.nypl.simplified.http.core;

import java.io.IOException;

import com.io7m.jnull.NullCheck;

public final class HTTPResultOK<A> implements HTTPResultOKType<A>
{
  private final String message;
  private final int    status;
  private final A      value;

  public HTTPResultOK(
    final String in_message,
    final int in_status,
    final A in_value)
  {
    this.message = NullCheck.notNull(in_message);
    this.status = in_status;
    this.value = NullCheck.notNull(in_value);
  }

  @Override public void close()
    throws IOException
  {
    // Nothing
  }

  @Override public String getMessage()
  {
    return this.message;
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
