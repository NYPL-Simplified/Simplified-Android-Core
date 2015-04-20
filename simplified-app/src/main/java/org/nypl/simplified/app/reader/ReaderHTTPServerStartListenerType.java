package org.nypl.simplified.app.reader;

/**
 * The type of server startup listeners.
 */

public interface ReaderHTTPServerStartListenerType
{
  void onServerStartFailed(
    ReaderHTTPServerType hs,
    Throwable x);

  void onServerStartSucceeded(
    ReaderHTTPServerType hs,
    boolean first);
}
