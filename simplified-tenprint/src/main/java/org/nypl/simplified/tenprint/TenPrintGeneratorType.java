package org.nypl.simplified.tenprint;

import android.graphics.Bitmap;

/**
 * The interface exposed by the 10 PRINT COVER generator.
 */

public interface TenPrintGeneratorType
{
  /**
   * Generate an image based on the given input parameters.
   * 
   * @param i
   *          The input parameters
   * @return A generated bitmap
   */

  Bitmap generate(
    TenPrintInput i);
}
