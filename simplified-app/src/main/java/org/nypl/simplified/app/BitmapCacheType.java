package org.nypl.simplified.app;

/**
 * The type of asynchronous bitmap caches.
 */

public interface BitmapCacheType
{
  /**
   * @return The size of data in the cache
   */

  long getSizeCurrent();

  /**
   * @return The maximum size of the cache
   */

  long getSizeMaximum();
}
