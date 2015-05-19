package org.nypl.simplified.app.reader;

public interface ReaderMediaOverlayAvailabilityListenerType
{
  /**
   * The media overlay is/is not available.
   */

  void onMediaOverlayIsAvailable(
    boolean available);

  /**
   * An error was raised upon attempting to query the state of the media
   * overlay.
   */

  void onMediaOverlayIsAvailableError(
    Throwable x);
}
