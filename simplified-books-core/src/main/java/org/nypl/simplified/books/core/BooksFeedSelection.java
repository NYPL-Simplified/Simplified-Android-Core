package org.nypl.simplified.books.core;

/**
 * A selector for generated feeds.
 */

public enum BooksFeedSelection
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
