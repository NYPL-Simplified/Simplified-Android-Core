package org.nypl.simplified.app.reader;

/**
 * A listener for receiving the current book location.
 */

public interface ReaderCurrentPageListenerType
{
  /**
   * The current book location could not be received.
   *
   * @param x The exception raised
   */

  void onCurrentPageError(
    Throwable x);

  /**
   * The current book location was received successfully.
   *
   * @param l The location
   */

  void onCurrentPageReceived(
    ReaderBookLocation l);
}
