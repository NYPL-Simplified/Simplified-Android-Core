package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;

public final class HTTPResultException<A> implements HTTPResultType<A>
{
  private final Throwable error;

  public HTTPResultException(
    final Throwable in_error)
  {
    this.error = NullCheck.notNull(in_error);
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
    final HTTPResultException<?> other = (HTTPResultException<?>) obj;
    return this.error.equals(other.error);
  }

  public Throwable getError()
  {
    return this.error;
  }

  @Override public int hashCode()
  {
    return this.error.hashCode();
  }

  @Override public <B, E extends Exception> B matchResult(
    final HTTPResultMatcherType<A, B, E> e)
    throws E
  {
    return e.onHTTPException(this);
  }
}
