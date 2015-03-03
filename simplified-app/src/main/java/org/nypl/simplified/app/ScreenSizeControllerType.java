package org.nypl.simplified.app;

/**
 * Information about the device screen.
 */

public interface ScreenSizeControllerType
{
  double screenDPToPixels(
    final int dp);

  double screenGetDPI();

  boolean screenIsLarge();
}
