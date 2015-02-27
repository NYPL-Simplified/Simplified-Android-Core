package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

public interface BitmapCacheScalingType extends BitmapCacheType
{
  /**
   * Fetch an image from the given URI and return a loaded {@link Bitmap}.
   *
   * @param uri
   *          The URI
   * @param p
   *          A listener
   * @return A future representing the loading operation
   */

  ListenableFuture<Bitmap> get(
    final URI uri,
    final BitmapScalingOptions opts,
    final BitmapCacheListenerType p);

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
    final BitmapScalingOptions opts)
    throws IOException;
}
