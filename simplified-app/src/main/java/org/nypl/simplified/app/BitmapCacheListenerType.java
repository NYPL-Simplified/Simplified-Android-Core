package org.nypl.simplified.app;

import android.graphics.Bitmap;

/**
 * The type of bitmap loading listeners.
 */

public interface BitmapCacheListenerType
{
  /**
   * The bitmap could not be loaded (or possibly could not even be fetched).
   *
   * @param e
   *          The error, if any
   */

  void onFailure(
    final Throwable e);

  /**
   * The bitmap was successfully loaded.
   *
   * @param b
   *          The resulting bitmap
   */

  void onSuccess(
    final Bitmap b);
}
