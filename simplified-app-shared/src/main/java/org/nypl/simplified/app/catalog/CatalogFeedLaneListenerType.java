package org.nypl.simplified.app.catalog;

import org.nypl.simplified.feeds.api.FeedEntry;
import org.nypl.simplified.feeds.api.FeedGroup;

/**
 * A listener that receives lane events.
 */

public interface CatalogFeedLaneListenerType {
  /**
   * A book was selected.
   *
   * @param e The given book
   */

  void onSelectBook(
    FeedEntry.FeedEntryOPDS e);

  /**
   * A feed group was selected.
   *
   * @param in_group The given group
   */

  void onSelectFeed(
    FeedGroup in_group);
}
