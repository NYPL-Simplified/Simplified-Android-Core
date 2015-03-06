package org.nypl.simplified.app;

import java.io.IOException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

public interface CatalogAcquisitionThumbnailCacheType
{
  /**
   * Fetch or generate an image asynchronously.
   *
   * @param e
   *          The acquisition feed entry
   * @return A loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  ListenableFuture<Bitmap> getThumbnailAsynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size,
    final BitmapCacheListenerType listener);

  /**
   * Fetch or generate an image synchronously.
   *
   * @param e
   *          The acquisition feed entry
   * @return A loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  Bitmap getThumbnailSynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size);
}
