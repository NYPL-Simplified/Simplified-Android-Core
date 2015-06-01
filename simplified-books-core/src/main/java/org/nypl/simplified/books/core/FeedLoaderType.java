package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.concurrent.Future;

import com.io7m.jfunctional.Unit;

/**
 * The type of feed loaders.
 */

public interface FeedLoaderType
{
  /**
   * Load a feed from the given URI, caching feeds that are successfully
   * fetched. The feed (or errors) are delivered to the given listener.
   *
   * @param uri
   *          The URI
   * @param listener
   *          The listener
   * @return A future that can be used to cancel the loading feed
   */

  Future<Unit> fromURI(
    URI uri,
    FeedLoaderListenerType listener);

  /**
   * Load a feed from the given URI, bypassing any cache, and caching feeds
   * that are successfully fetched. The feed (or errors) are delivered to the
   * given listener.
   *
   * @param uri
   *          The URI
   * @param listener
   *          The listener
   * @return A future that can be used to cancel the loading feed
   */

  Future<Unit> fromURIRefreshing(
    URI uri,
    FeedLoaderListenerType listener);

  /**
   * Invalidate the cached feed for URI <tt>uri</tt>, if any.
   *
   * @param uri
   *          The URI
   */

  void invalidate(
    URI uri);
}
