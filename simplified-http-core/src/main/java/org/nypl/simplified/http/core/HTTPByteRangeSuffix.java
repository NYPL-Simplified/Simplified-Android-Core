package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.assertions.Assertions;

/**
 * The byte range of an HTTP Range request.
 */

public final class HTTPByteRangeSuffix implements HTTPRangeType
{
  private final long byte_from_end;

  /**
   * Construct a suffix byte range.
   *
   * @param in_byte_from_end The number of bytes from the end
   */

  public HTTPByteRangeSuffix(
    final long in_byte_from_end)
  {
    Assertions.checkPrecondition(
      in_byte_from_end >= 0L,
      "byte_from_end %d >= 0",
      Long.valueOf(in_byte_from_end));
    this.byte_from_end = in_byte_from_end;
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
    final HTTPByteRangeSuffix other = (HTTPByteRangeSuffix) obj;
    return this.byte_from_end == other.byte_from_end;
  }

  /**
   * @return The number of bytes from the end
   */

  public long getByteFromEnd()
  {
    return this.byte_from_end;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + (int) (this.byte_from_end ^ (this.byte_from_end
                                                             >>> 32));
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(32);
    b.append("-");
    b.append(this.byte_from_end);
    return NullCheck.notNull(b.toString());
  }

  @Override public <A, E extends Exception> A matchRangeType(
    final HTTPRangeMatcherType<A, E> m)
    throws E
  {
    return m.onHTTPByteRangeSuffix(this);
  }
}
