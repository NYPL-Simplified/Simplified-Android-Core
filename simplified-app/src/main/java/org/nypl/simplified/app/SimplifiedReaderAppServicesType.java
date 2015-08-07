package org.nypl.simplified.app;

import org.nypl.simplified.app.reader.ReaderBookmarksType;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.app.reader.ReaderSettingsType;

/**
 * Services provided to the reader.
 */

public interface SimplifiedReaderAppServicesType
  extends ScreenSizeControllerType
{
  /**
   * @return The bookmarks database
   */

  ReaderBookmarksType getBookmarks();

  /**
   * @return The EPUB loader
   */

  ReaderReadiumEPUBLoaderType getEPUBLoader();

  /**
   * @return The HTTP server
   */

  ReaderHTTPServerType getHTTPServer();

  /**
   * @return The settings database
   */

  ReaderSettingsType getSettings();
}
