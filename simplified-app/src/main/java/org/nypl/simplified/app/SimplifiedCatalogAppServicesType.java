package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedLoaderType;

/**
 * Services provided to the main Simplified app.
 */

public interface SimplifiedCatalogAppServicesType extends
  ScreenSizeControllerType,
  NetworkConnectivityType,
  SimplifiedAppInitialSyncType
{
  /**
   * @return A book management interface
   */

  BooksType getBooks();

  /**
   * @return A cover provider
   */

  BookCoverProviderType getCoverProvider();

  /**
   * @return The initial URI for the catalog
   */

  URI getFeedInitialURI();

  /**
   * @return An asynchronous feed loader
   */

  FeedLoaderType getFeedLoader();

}
