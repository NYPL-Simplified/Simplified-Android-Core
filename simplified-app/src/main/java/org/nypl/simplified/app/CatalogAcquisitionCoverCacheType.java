package org.nypl.simplified.app;

import java.io.IOException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

public interface CatalogAcquisitionCoverCacheType
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

  ListenableFuture<Bitmap> getCoverAsynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size,
    final BitmapCacheListenerType<OPDSAcquisitionFeedEntry> listener);

  /**
   * Fetch or generate an image synchronously.
   *
   * @param e
   *          The acquisition feed entry
   * @return A loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  Bitmap getCoverSynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size);
}
