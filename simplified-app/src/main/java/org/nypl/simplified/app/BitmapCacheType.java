package org.nypl.simplified.app;

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
}
