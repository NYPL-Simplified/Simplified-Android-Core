package org.nypl.simplified.feeds.api;

import java.io.Serializable;

/**
 * A selector for generated feeds.
 */

public enum FeedBooksSelection implements Serializable
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
