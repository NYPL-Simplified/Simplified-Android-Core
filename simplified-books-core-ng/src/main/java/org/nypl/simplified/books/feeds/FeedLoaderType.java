package org.nypl.simplified.books.feeds;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.http.core.HTTPAuthType;

import java.net.URI;

/**
 * The type of feed loaders.
 */

public interface FeedLoaderType
{
  /**
   * Load a feed from the given URI, caching feeds that are successfully
   * fetched. The feed (or errors) are delivered to the given listener.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   * @param listener The listener
   *
   * @return A future that can be used to cancel the loading feed
   */

  ListenableFuture<FeedType> fromURI(
    URI uri,
    OptionType<HTTPAuthType> auth,
    FeedLoaderListenerType listener);

  /**
   * Load a feed from the given URI, bypassing any cache, and caching feeds that
   * are successfully fetched. The feed (or errors) are delivered to the given
   * listener.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   * @param method   HTTP method to use (GET/PUT)
   * @param listener The listener
   *
   * @return A future that can be used to cancel the loading feed
   */

  ListenableFuture<FeedType> fromURIRefreshing(
    URI uri,
    OptionType<HTTPAuthType> auth,
    String method,
    FeedLoaderListenerType listener);

  /**
   * Load a feed from the given URI, caching feeds that are successfully
   * fetched. The feed (or errors) are delivered to the given listener. For each
   * returned entry in the feed, the local book database is examined and any
   * matching entries are replaced with the data most recently written into the
   * database.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   * @param listener The listener
   *
   * @return A future that can be used to cancel the loading feed
   */

  ListenableFuture<FeedType> fromURIWithBookRegistryEntries(
    URI uri,
    OptionType<HTTPAuthType> auth,
    FeedLoaderListenerType listener);

  /**
   * Invalidate the cached feed for URI {@code uri}, if any.
   *
   * @param uri The URI
   */

  void invalidate(
    URI uri);
}
