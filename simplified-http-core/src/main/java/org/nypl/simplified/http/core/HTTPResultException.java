package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.net.URI;

/**
 * An HTTP result representing a typically client-side error.
 *
 * @param <A> The type of result value
 */

public final class HTTPResultException<A> implements HTTPResultType<A>
{
  private final Exception error;
  private final URI       uri;

  /**
   * Construct a result value.
   *
   * @param in_uri   The URI
   * @param in_error The exception raised
   */

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

  /**
   * @return The exception raised
   */

  public Exception getError()
  {
    return this.error;
  }

  /**
   * @return The URI
   */

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
