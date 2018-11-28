package org.nypl.simplified.app;

import com.io7m.jfunctional.OptionType;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.accessibility.AccessibilityType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.covers.BookCoverProviderType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.multilibrary.Account;

/**
 * Services provided to the main Simplified app.
 */

public interface SimplifiedCatalogAppServicesType extends
  ScreenSizeControllerType,
  NetworkConnectivityType,
  SimplifiedAppInitialSyncType
{
  /**
   * @return A reference to the accessibility system
   */

  AccessibilityType getAccessibility();

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
   * @return An HTTP request provider
   */

  HTTPType getHTTP();

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
   * @param delete_books  should book shelf be deleted
   * @param account which accounts book shelf should be deleted
   */
  void reloadCatalog(boolean delete_books, Account account);

  /**
   *
   */
  void destroyDatabase();
}
