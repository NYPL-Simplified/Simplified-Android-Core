package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A mapping from {@link URI} to {@link InputStream}.
 *
 * Mostly included to facilitate unit testing without needing an HTTP server
 * to exist.
 */

public interface OPDSFeedTransportType
{
  InputStream getStream(
    final URI uri)
      throws IOException;
}
