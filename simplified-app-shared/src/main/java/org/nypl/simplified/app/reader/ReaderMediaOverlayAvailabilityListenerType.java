package org.nypl.simplified.app.reader;

/**
 * A listener that will be notified of media overlay availability.
 */

public interface ReaderMediaOverlayAvailabilityListenerType
{
  /**
   * The media overlay is/is not available.
   *
   * @param available {@code true} if the overlay is available
   */

  void onMediaOverlayIsAvailable(
    boolean available);

  /**
   * An error was raised upon attempting to query the state of the media
   * overlay.
   *
   * @param x The raised error
   */

  void onMediaOverlayIsAvailableError(
    Throwable x);
}
