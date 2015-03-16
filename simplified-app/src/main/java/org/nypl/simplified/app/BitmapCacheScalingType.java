package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;

public interface BitmapCacheScalingType extends BitmapCacheType
{
  /**
   * Fetch an image synchronously. This is typically only useful for calling
   * by other caches.
   *
   * @param uri
   *          The URI
   * @return The loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  Bitmap getSynchronous(
    final URI uri,
    final BitmapDisplaySizeType size)
    throws IOException;
}
