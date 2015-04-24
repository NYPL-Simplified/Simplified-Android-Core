package org.nypl.simplified.app;

import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;

/**
 * Services provided to the reader.
 */

public interface SimplifiedReaderAppServicesType extends
  ScreenSizeControllerType
{
  ReaderReadiumEPUBLoaderType getEPUBLoader();

  ReaderHTTPServerType getHTTPServer();
}
