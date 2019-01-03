package org.nypl.simplified.books.feeds;

/**
 * A selector for generated feeds.
 */

public enum FeedBooksSelection
{
  /**
   * Generate a feed of loaned books.
   */

  BOOKS_FEED_LOANED,

  /**
   * Generate a feed of books that are currently on hold.
   */

  BOOKS_FEED_HOLDS
}
