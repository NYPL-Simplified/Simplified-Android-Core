package org.nypl.simplified.http.core;

import org.nypl.simplified.assertions.Assertions;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The byte range of an HTTP Range request.
 */

@SuppressWarnings("boxing") public final class HTTPByteRangeInclusive implements
  HTTPRangeType
{
  private final long byte_end;
  private final long byte_start;

  public HTTPByteRangeInclusive(
    final long in_byte_start,
    final long in_byte_end)
  {
    Assertions.checkPrecondition(
      in_byte_start >= 0,
      "byte_start %d >= 0",
      in_byte_start);
    this.byte_start = in_byte_start;
    this.byte_end = in_byte_end;
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
    final HTTPByteRangeInclusive other = (HTTPByteRangeInclusive) obj;
    return (this.byte_end == other.byte_end)
      && (this.byte_start == other.byte_start);
  }

  public long getByteEnd()
  {
    return this.byte_end;
  }

  public long getByteStart()
  {
    return this.byte_start;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result =
      (prime * result) + (int) (this.byte_end ^ (this.byte_end >>> 32));
    result =
      (prime * result) + (int) (this.byte_start ^ (this.byte_start >>> 32));
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append(this.byte_start);
    b.append("-");
    if (this.byte_end >= 0) {
      b.append(this.byte_end);
    }
    return NullCheck.notNull(b.toString());
  }

  @Override public <A, E extends Exception> A matchRangeType(
    final HTTPRangeMatcherType<A, E> m)
    throws E
  {
    return m.onHTTPByteRangeInclusive(this);
  }
}
