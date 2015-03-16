package org.nypl.simplified.app;

public interface BitmapDisplaySizeMatcherType<A, E extends Exception>
{
  A matchHeightAspectPreserving(
    BitmapDisplayHeightPreserveAspect s)
    throws E;
}
