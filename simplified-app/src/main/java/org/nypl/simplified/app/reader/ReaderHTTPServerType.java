package org.nypl.simplified.app.reader;

/**
 * The interface exposed by the reader HTTP server.
 */

public interface ReaderHTTPServerType
{
  void start(
    ReaderHTTPServerStartListenerType s);
}
