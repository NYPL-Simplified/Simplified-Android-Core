package org.nypl.simplified.app;

import android.graphics.Bitmap;

/**
 * The type of bitmap cache loading listeners.
 *
 * @param <K>
 *          The type of cache keys
 */

public interface BitmapCacheListenerType<K>
{
  /**
   * A bitmap failed to load for <tt>key</tt>.
   *
   * @param key
   *          The key
   * @param x
   *          The error
   */

  void onBitmapLoadingFailure(
    final K key,
    final Throwable x);

  /**
   * A bitmap was successfully loaded for <tt>key</tt>.
   *
   * @param key
   *          The key
   * @param b
   *          The bitmap
   */

  void onBitmapLoadingSuccess(
    final K key,
    final Bitmap b);
}
