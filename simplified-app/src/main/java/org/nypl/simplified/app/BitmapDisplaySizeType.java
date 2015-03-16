package org.nypl.simplified.app;

public interface BitmapDisplaySizeType
{
  <A, E extends Exception> A matchSize(
    final BitmapDisplaySizeMatcherType<A, E> m)
    throws E;
}
