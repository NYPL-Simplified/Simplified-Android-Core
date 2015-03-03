package org.nypl.simplified.app;

public final class BitmapDisplayHeightPreserveAspect implements
  BitmapDisplaySizeType
{
  private final int height;

  public BitmapDisplayHeightPreserveAspect(
    final int in_height)
  {
    this.height = in_height;
  }

  public int getHeight()
  {
    return this.height;
  }

  @Override public <A, E extends Exception> A matchSize(
    final BitmapDisplaySizeMatcherType<A, E> m)
    throws E
  {
    return m.matchHeightAspectPreserving(this);
  }
}
