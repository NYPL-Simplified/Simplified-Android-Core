package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;

/**
 * Services provided to the main Simplified app.
 */

public interface SimplifiedCatalogAppServicesType extends
  ScreenSizeControllerType,
  SimplifiedAppInitialSyncType
{
  /**
   * @return A book management interface
   */

  BooksType getBooks();

  /**
   * @return A cover provider
   */

  CoverProviderType getCoverProvider();

  /**
   * @return The initial URI for the catalog
   */

  URI getFeedInitialURI();

  /**
   * @return An asynchronous feed loader
   */

  OPDSFeedLoaderType getFeedLoader();

}
