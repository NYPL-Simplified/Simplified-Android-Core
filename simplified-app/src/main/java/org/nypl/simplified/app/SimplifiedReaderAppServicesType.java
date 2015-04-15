package org.nypl.simplified.app;

import org.nypl.simplified.app.reader.ReaderHTTPServerType;

/**
 * Services provided to the reader.
 */

public interface SimplifiedReaderAppServicesType
{
  ReaderHTTPServerType getHTTPServer();
}
