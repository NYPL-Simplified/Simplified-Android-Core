package org.nypl.simplified.app;

import com.io7m.jfunctional.OptionType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
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
   * @return A reference to the document store.
   */

  DocumentStoreType getDocumentStore();

  /**
   * @return A book management interface
   */

  BooksType getBooks();

  /**
   * @return A cover provider
   */

  BookCoverProviderType getCoverProvider();

  /**
   * @return An asynchronous feed loader
   */

  FeedLoaderType getFeedLoader();

  /**
   * @return Adobe DRM services, if any are available
   */

  OptionType<AdobeAdeptExecutorType> getAdobeDRMExecutor();

  /**
   * @return HelpStack services, if any are available
   */

  OptionType<HelpstackType> getHelpStack();

  /**
   * 
   */
  void reloadCatalog();

}
