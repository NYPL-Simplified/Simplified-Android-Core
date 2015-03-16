package org.nypl.simplified.app;

import com.io7m.jnull.Nullable;

public final class BitmapDisplayHeightPreserveAspect implements
  BitmapDisplaySizeType
{
  private final int height;

  public BitmapDisplayHeightPreserveAspect(
    final int in_height)
  {
    this.height = in_height;
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
    final BitmapDisplayHeightPreserveAspect other =
      (BitmapDisplayHeightPreserveAspect) obj;
    return (this.height == other.height);
  }

  public int getHeight()
  {
    return this.height;
  }

  @Override public int hashCode()
  {
    return Integer.valueOf(this.height).hashCode();
  }

  @Override public <A, E extends Exception> A matchSize(
    final BitmapDisplaySizeMatcherType<A, E> m)
    throws E
  {
    return m.matchHeightAspectPreserving(this);
  }
}
