package org.nypl.simplified.books.core;

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
   * A feed failed to load.
   *
   * @param u The URI of the feed
   * @param x The exception raised
   */

  void onFeedLoadFailure(
    URI u,
    Throwable x);
}
