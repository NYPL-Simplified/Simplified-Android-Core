package org.nypl.simplified.app.reader;

import java.net.URI;

import org.readium.sdk.android.Package;

/**
 * The interface exposed by the reader HTTP server.
 */

public interface ReaderHTTPServerType
{
  URI getURIBase();

  void startIfNecessaryForPackage(
    Package p,
    ReaderHTTPServerStartListenerType s);
}
