package org.nypl.simplified.app.reader;

/**
 * The type of server startup listeners.
 */

public interface ReaderHTTPServerStartListenerType
{
  void onServerStartSucceeded(
    ReaderHTTPServerType hs);

  void onServerStartFailed(
    ReaderHTTPServerType hs,
    Throwable x);
}
