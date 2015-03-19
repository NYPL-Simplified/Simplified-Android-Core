package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class HTTPResultException<A> implements HTTPResultType<A>
{
  private final Exception error;

  public HTTPResultException(
    final Exception in_error)
  {
    this.error = NullCheck.notNull(in_error);
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
    final HTTPResultException<?> other = (HTTPResultException<?>) obj;
    return this.error.equals(other.error);
  }

  public Exception getError()
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
