package org.nypl.simplified.app;

import org.nypl.simplified.app.reader.ReaderBookmarksType;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.app.reader.ReaderSettingsType;

/**
 * Services provided to the reader.
 */

public interface SimplifiedReaderAppServicesType extends
  ScreenSizeControllerType
{
  ReaderBookmarksType getBookmarks();

  ReaderReadiumEPUBLoaderType getEPUBLoader();

  ReaderHTTPServerType getHTTPServer();

  ReaderSettingsType getSettings();
}
