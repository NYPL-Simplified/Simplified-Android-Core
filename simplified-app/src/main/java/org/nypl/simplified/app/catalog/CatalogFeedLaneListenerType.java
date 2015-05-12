package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.FeedBlock;
import org.nypl.simplified.books.core.FeedEntryOPDS;

public interface CatalogFeedLaneListenerType
{
  void onSelectFeed(
    FeedBlock in_block);

  void onSelectBook(
    FeedEntryOPDS e);
}
