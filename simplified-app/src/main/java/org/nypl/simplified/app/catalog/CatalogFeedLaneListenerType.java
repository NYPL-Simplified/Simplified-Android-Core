package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.FeedGroup;
import org.nypl.simplified.books.core.FeedEntryOPDS;

public interface CatalogFeedLaneListenerType
{
  void onSelectBook(
    FeedEntryOPDS e);

  void onSelectFeed(
    FeedGroup in_block);
}
