package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * The type of asynchronous bitmap caches.
 */

public interface BitmapCacheType
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
    final BitmapCacheListenerType p);

  /**
   * @return The size of data in the cache
   */

  long getSizeCurrent();

  /**
   * @return The maximum size of the cache
   */

  long getSizeMaximum();

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
    final URI uri)
    throws IOException;
}
