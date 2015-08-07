package org.nypl.simplified.app;

/**
 * Information about the device screen.
 */

public interface ScreenSizeControllerType
{
  /**
   * @param dp A size in dp
   *
   * @return The given size converted to pixels
   */

  double screenDPToPixels(
    final int dp);

  /**
   * @return The DPI of the current screen
   */

  double screenGetDPI();

  /**
   * @return The height of the screen in pixels
   */

  int screenGetHeightPixels();

  /**
   * @return The width of the screen in pixels
   */

  int screenGetWidthPixels();

  /**
   * @return {@code true} if the current screen is considered to be "large"
   */

  boolean screenIsLarge();
}
