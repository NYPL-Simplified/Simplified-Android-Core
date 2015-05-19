package org.nypl.simplified.http.core;

import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class HTTPResultException<A> implements HTTPResultType<A>
{
  private final Exception error;
  private final URI       uri;

  public HTTPResultException(
    final URI in_uri,
    final Exception in_error)
  {
    this.uri = NullCheck.notNull(in_uri);
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
    return this.error.equals(other.error) && this.uri.equals(other.uri);
  }

  public Exception getError()
  {
    return this.error;
  }

  public URI getURI()
  {
    return this.uri;
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
