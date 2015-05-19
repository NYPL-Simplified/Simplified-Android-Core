package org.nypl.simplified.books.core;

/**
 * The type of listeners for generating book feeds.
 */

public interface BookFeedListenerType
{
  /**
   * Generating the feed failed.
   */

  void onBookFeedFailure(
    Throwable x);

  /**
   * Generating the feed succeeded.
   */

  void onBookFeedSuccess(
    FeedWithoutGroups f);
}
