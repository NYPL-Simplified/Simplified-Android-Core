package org.nypl.simplified.app.reader;

import org.readium.sdk.android.Package;

import java.net.URI;

/**
 * The interface exposed by the reader HTTP server.
 */

public interface ReaderHTTPServerType
{
  /**
   * @return The base URI used to request resources from the server.
   */

  URI getURIBase();

  /**
   * Start the server if it is not already running, and instruct it to serve the
   * given package. If the server is already running, it will serve the given
   * package for all subsequent requests.
   *
   * @param p The EPUB package
   * @param s The server listener
   */

  void startIfNecessaryForPackage(
    Package p,
    ReaderHTTPServerStartListenerType s);
}
