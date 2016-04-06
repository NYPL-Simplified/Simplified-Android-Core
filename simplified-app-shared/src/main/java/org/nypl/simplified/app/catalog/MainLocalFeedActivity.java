package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * An abstract activity implementing the common feed refreshing code for Books
 * and Holds activities.
 */

abstract class MainLocalFeedActivity extends CatalogFeedActivity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainLocalFeedActivity.class);
  }

}
