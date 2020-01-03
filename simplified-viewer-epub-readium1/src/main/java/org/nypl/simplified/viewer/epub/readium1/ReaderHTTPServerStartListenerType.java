package org.nypl.simplified.viewer.epub.readium1;

/**
 * The type of server startup listeners.
 */

public interface ReaderHTTPServerStartListenerType
{
  /**
   * The server failed to start.
   *
   * @param hs The server
   * @param x  The error raised
   */

  void onServerStartFailed(
    ReaderHTTPServerType hs,
    Throwable x);

  /**
   * The server started successfully. The value of {@code first} is {@code true}
   * if this is the first time the server has been requested to start.
   *
   * @param hs    The server
   * @param first Indicates whether this is the first server startup
   */

  void onServerStartSucceeded(
    ReaderHTTPServerType hs,
    boolean first);
}
