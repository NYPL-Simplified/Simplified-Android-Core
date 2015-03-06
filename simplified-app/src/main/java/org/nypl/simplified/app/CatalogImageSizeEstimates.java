package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

public final class CatalogImageSizeEstimates
{
  /**
   * Estimate a thumbnail height in pixels that would lead to roughly 3
   * thumbnails fitting onscreen if stacked vertically as they are in
   * acquisition feed lanes on non-tablet devices.
   *
   * The scaling value was found via experimentation and does not have any
   * particular meaning.
   */

  public static int acquisitionFeedLargeThumbnailHeight(
    final ScreenSizeControllerType screen)
  {
    final double scale = 4;
    final int height =
      (int) (NullCheck.notNull(screen).screenGetHeightPixels() / scale);
    return height;
  }

  /**
   * Estimate a thumbnail height in pixels that would lead to roughly 5-6
   * thumbnails fitting onscreen if stacked vertically as they are in
   * navigation feed lanes. This tends to show fewer lanes on extremely small
   * screens (which may not matter; how many QVGA devices are there that
   * support Android 4.4 and that people are actually using in 2015?).
   *
   * The scaling value was found via experimentation and does not have any
   * particular meaning.
   */

  public static int navigationFeedThumbnailHeight(
    final ScreenSizeControllerType screen)
  {
    final double scale = 7.45;
    final int height =
      (int) (NullCheck.notNull(screen).screenGetHeightPixels() / scale);
    return height;
  }
}
