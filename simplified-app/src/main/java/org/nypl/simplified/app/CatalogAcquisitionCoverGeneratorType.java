package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;

public interface CatalogAcquisitionCoverGeneratorType
{
  /**
   * Generate an image synchronously.
   *
   * @param u
   *          The image URI
   * @return A loaded bitmap
   * @throws IOException
   *           On I/O errors
   */

  Bitmap generateImage(
    URI u,
    int width,
    int height);
}
