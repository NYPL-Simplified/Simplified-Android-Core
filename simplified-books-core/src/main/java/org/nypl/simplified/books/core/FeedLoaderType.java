package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSSearchParserType;

import java.net.URI;
import java.util.concurrent.Future;

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

  Future<Unit> fromURI(
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
   * @param listener The listener
   *
   * @return A future that can be used to cancel the loading feed
   */

  Future<Unit> fromURIRefreshing(
    URI uri,
    OptionType<HTTPAuthType> auth,
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

  Future<Unit> fromURIWithDatabaseEntries(
    URI uri,
    OptionType<HTTPAuthType> auth,
    FeedLoaderListenerType listener);

  /**
   * Load a feed from the given URI, bypassing any cache, and caching feeds that
   * are successfully fetched. The feed (or errors) are delivered to the given
   * listener. For each returned entry in the feed, the local book database is
   * examined and any matching entries are replaced with the data most recently
   * written into the database.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   * @param listener The listener
   *
   * @return A future that can be used to cancel the loading feed
   */

  Future<Unit> fromURIRefreshingWithDatabaseEntries(
    URI uri,
    OptionType<HTTPAuthType> auth,
    FeedLoaderListenerType listener);

  /**
   * @return The feed parser that backs this loader.
   */

  OPDSFeedParserType getOPDSFeedParser();

  /**
   * @return The feed transport that backs this loader.
   */

  OPDSFeedTransportType<OptionType<HTTPAuthType>> getOPDSFeedTransport();

  /**
   * @return The search parser that backs this loader.
   */

  OPDSSearchParserType getOPDSSearchParser();

  /**
   * Invalidate the cached feed for URI {@code uri}, if any.
   *
   * @param uri The URI
   */

  void invalidate(
    URI uri);
}
