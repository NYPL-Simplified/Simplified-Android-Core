package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.feeds.FeedGroup;

import static org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS;

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
    FeedEntryOPDS e);

  /**
   * A feed group was selected.
   *
   * @param in_group The given group
   */

  void onSelectFeed(
    FeedGroup in_group);
}
