package org.nypl.simplified.app.reader;

/**
 * The type of server startup listeners.
 */

public interface ReaderHTTPServerStartListenerType
{
  /**
   * The server failed to start.
   */

  void onServerStartFailed(
    ReaderHTTPServerType hs,
    Throwable x);

  /**
   * The server started successfully. The value of <tt>first</tt> is
   * <tt>true</tt> if this is the first time the server has been requested to
   * start.
   */

  void onServerStartSucceeded(
    ReaderHTTPServerType hs,
    boolean first);
}
