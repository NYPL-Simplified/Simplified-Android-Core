package org.nypl.simplified.app;

import java.io.IOException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;

public interface CatalogAcquisitionThumbnailCacheType
{
  /**
   * Fetch or generate an image synchronously.
   *
   * @param e
   *          The acquisition feed entry
   * @return A loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  Bitmap getSynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size);
}
