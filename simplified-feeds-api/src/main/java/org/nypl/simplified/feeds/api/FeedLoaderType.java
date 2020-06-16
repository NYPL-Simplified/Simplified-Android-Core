package org.nypl.simplified.feeds.api;

import com.google.common.util.concurrent.FluentFuture;
import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.http.core.HTTPAuthType;

import java.net.URI;

/**
 * The type of feed loaders.
 */

public interface FeedLoaderType
{
  /**
   * Load a feed from the given URI, caching feeds that are successfully
   * fetched.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   *
   * @return A future that can be used to cancel the loading feed
   */

  FluentFuture<FeedLoaderResult> fetchURI(
    AccountID account,
    URI uri,
    OptionType<HTTPAuthType> auth
  );

  /**
   * Load a feed from the given URI, bypassing any cache, and caching feeds that
   * are successfully fetched.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   * @param method   HTTP method to use (GET/PUT)
   *
   * @return A future that can be used to cancel the loading feed
   */

  FluentFuture<FeedLoaderResult> fetchURIRefreshing(
    AccountID account,
    URI uri,
    OptionType<HTTPAuthType> auth,
    String method
  );

  /**
   * Load a feed from the given URI, caching feeds that are successfully
   * fetched. For each returned entry in the feed, the local book database is examined
   * and any matching entries are replaced with the data most recently written into the
   * database.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   *
   * @return A future that can be used to cancel the loading feed
   */

  FluentFuture<FeedLoaderResult> fetchURIWithBookRegistryEntries(
    AccountID account,
    URI uri,
    OptionType<HTTPAuthType> auth
  );

  /**
   * Invalidate the cached feed for URI {@code uri}, if any.
   *
   * @param uri The URI
   */

  void invalidate(
    URI uri);
}
