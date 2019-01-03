package org.nypl.simplified.books.feeds;

import java.net.URI;

/**
 * The type of feed loading listeners.
 */

public interface FeedLoaderListenerType
{
  /**
   * A feed loaded successfully.
   *
   * @param u The URI of the feed
   * @param f The feed
   */

  void onFeedLoadSuccess(
    URI u,
    FeedType f);

  /**
   * A feed requires authentication details.
   *
   * @param u        The URI of the feed
   * @param attempts The number of times that credentials have been requested
   *                 during this attempt to load this particular feed
   * @param listener The listener that will receive authentication data
   */

  void onFeedRequiresAuthentication(
    URI u,
    int attempts,
    FeedLoaderAuthenticationListenerType listener);

  /**
   * A feed failed to load.
   *
   * @param u The URI of the feed
   * @param x The exception raised
   */

  void onFeedLoadFailure(
    URI u,
    Throwable x);
}
