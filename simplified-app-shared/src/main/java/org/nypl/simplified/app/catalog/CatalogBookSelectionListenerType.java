package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.feeds.FeedEntryOPDS;

/**
 * The type of book selection listeners.
 */

public interface CatalogBookSelectionListenerType {

  /**
   * The user selected a book.
   *
   * @param v The cell
   * @param e The selected book
   */

  void onSelectBook(
    final CatalogFeedBookCellView v,
    final FeedEntryOPDS e);
}
