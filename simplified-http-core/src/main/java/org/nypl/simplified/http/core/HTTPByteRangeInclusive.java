package org.nypl.simplified.http.core;

import com.google.common.base.Preconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The byte range of an HTTP Range request.
 */

@SuppressWarnings("boxing") public final class HTTPByteRangeInclusive
  implements HTTPRangeType
{
  private final long byte_end;
  private final long byte_start;

  /**
   * Construct a byte range.
   *
   * @param in_byte_start The starting byte
   * @param in_byte_end   The ending byte
   */

  public HTTPByteRangeInclusive(
    final long in_byte_start,
    final long in_byte_end)
  {
    Preconditions.checkArgument(
      in_byte_start >= 0L, "byte_start %d >= 0", in_byte_start);
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
    return (this.byte_end == other.byte_end) && (this.byte_start
                                                 == other.byte_start);
  }

  /**
   * @return The ending byte
   */

  public long getByteEnd()
  {
    return this.byte_end;
  }

  /**
   * @return The starting byte
   */

  public long getByteStart()
  {
    return this.byte_start;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + (int) (this.byte_end ^ (this.byte_end >>> 32));
    result =
      (prime * result) + (int) (this.byte_start ^ (this.byte_start >>> 32));
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(32);
    b.append(this.byte_start);
    b.append("-");
    if (this.byte_end >= 0L) {
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
