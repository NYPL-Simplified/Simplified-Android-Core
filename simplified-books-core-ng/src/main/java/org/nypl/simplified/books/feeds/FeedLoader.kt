package org.nypl.simplified.books.feeds

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import net.jodah.expiringmap.ExpiringMap
import net.jodah.expiringmap.ExpiringMap.ExpirationListener
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.bundled_content.BundledURIs
import org.nypl.simplified.books.feeds.Feed.FeedWithGroups
import org.nypl.simplified.books.feeds.Feed.FeedWithoutGroups
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderFailure
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import org.nypl.simplified.opds.core.OPDSSearchLink
import org.nypl.simplified.opds.core.OPDSSearchParserType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * The default implementation of the [FeedLoaderType] interface.
 *
 * This implementation caches feeds. A feed is ejected from the cache if it has
 * not been accessed for five minutes.
 */

class FeedLoader private constructor(
  private val cache: ExpiringMap<URI, Feed>,
  private val exec: ListeningExecutorService,
  private val parser: OPDSFeedParserType,
  private val searchParser: OPDSSearchParserType,
  private val transport: OPDSFeedTransportType<OptionType<HTTPAuthType>>,
  private val bookRegistry: BookRegistryReadableType,
  private val bundledContent: BundledContentResolverType) : FeedLoaderType, ExpirationListener<URI, Feed> {

  init {
    this.cache.addExpirationListener(this)
  }

  private val log = LoggerFactory.getLogger(FeedLoader::class.java)


  private fun fetchURICore(
    uri: URI,
    auth: OptionType<HTTPAuthType>,
    updateFromRegistry: Boolean): FluentFuture<FeedLoaderResult> {

    if (this.cache.containsKey(uri)) {
      return FluentFuture.from(Futures.immediateFuture(
        FeedLoaderSuccess(this.cache[uri]!!) as FeedLoaderResult))
    }

    return FluentFuture.from(this.exec.submit(Callable {
      fetchSynchronously(uri, auth, "GET", updateFromRegistry = updateFromRegistry)
    }))
  }

  override fun fetchURI(
    uri: URI,
    auth: OptionType<HTTPAuthType>): FluentFuture<FeedLoaderResult> {
    return fetchURICore(uri, auth, updateFromRegistry = false)
  }

  override fun fetchURIRefreshing(
    uri: URI,
    auth: OptionType<HTTPAuthType>,
    method: String): FluentFuture<FeedLoaderResult> {
    this.invalidate(uri)
    return fetchURICore(uri, auth, updateFromRegistry = false)
  }

  override fun fetchURIWithBookRegistryEntries(
    uri: URI,
    auth: OptionType<HTTPAuthType>): FluentFuture<FeedLoaderResult> {
    return fetchURICore(uri, auth, updateFromRegistry = true)
  }

  override fun invalidate(uri: URI) {
    this.cache.remove(uri)
  }

  override fun expired(key: URI, value: Feed) {
    this.log.debug("expired feed: {}", key)
  }

  private fun fetchSynchronously(
    uri: URI,
    auth: OptionType<HTTPAuthType>,
    method: String,
    updateFromRegistry: Boolean): FeedLoaderResult {

    try {

      /*
       * If the URI has a scheme that refers to bundled content, fetch the data from
       * the resolver instead.
       */

      if (BundledURIs.isBundledURI(uri)) {
        return parseFromBundledContent(uri)
      }

      /*
       * Otherwise, parse the OPDS feed including any embedded search links.
       */

      val opdsFeed =
        this.transport.getStream(auth, uri, method).use { stream -> this.parser.parse(uri, stream) }
      val search =
        fetchSearchLink(opdsFeed, auth, method, uri)

      val feed = Feed.fromAcquisitionFeed(opdsFeed, search)

      /*
       * Replace entries in the feed with those from the book registry, if requested.
       */

      if (updateFromRegistry) {
        this.updateFeedFromBookRegistry(feed)
      }

      this.cache[uri] = feed
      return FeedLoaderSuccess(feed)
    } catch (e: FeedHTTPTransportException) {
      if (e.code == 401) {
        return FeedLoaderFailure.FeedLoaderFailedAuthentication(e)
      }
      return FeedLoaderFailure.FeedLoaderFailedGeneral(e)
    } catch (e: Exception) {
      return FeedLoaderFailure.FeedLoaderFailedGeneral(e)
    }
  }

  private fun parseFromBundledContent(uri: URI): FeedLoaderSuccess {
    return this.bundledContent.resolve(uri).use { stream ->
      FeedLoaderSuccess(Feed.fromAcquisitionFeed(this.parser.parse(uri, stream), null))
    }
  }

  private fun fetchSearchLink(
    opdsFeed: OPDSAcquisitionFeed,
    auth: OptionType<HTTPAuthType>,
    method: String,
    uri: URI): OPDSOpenSearch1_1? {
    val searchLinkOpt = opdsFeed.feedSearchURI
    return if (searchLinkOpt is Some<OPDSSearchLink>) {
      val searchLink = searchLinkOpt.get()
      this.transport.getStream(auth, searchLink.uri, method).use { stream ->
        return this.searchParser.parse(searchLink.uri, stream)
      }
    } else {
      null
    }
  }

  private fun updateFeedFromBookRegistry(feed: Feed) {
    when (feed) {
      is FeedWithoutGroups -> {
        this.log.debug("updating {} entries (without groups) from book registry", feed.size)
        for (index in 0 until feed.size) {
          val e = feed.entriesInOrder.get(index)
          val id = e.bookID
          val bookWithStatus = this.bookRegistry.books().get(id)
          if (bookWithStatus != null) {
            log.debug("updating entry {} from book registry", id)
            val en = FeedEntry.FeedEntryOPDS(bookWithStatus.book().entry)
            feed.entriesInOrder.set(index, en)
          }
        }
      }
      is FeedWithGroups -> {
        this.log.debug("updating {} entries (with groups) from book registry", feed.size)
        for (index in 0 until feed.size) {
          val group = feed.feedGroupsInOrder.get(index)
          val entries = group.getGroupEntries()
          for (gi in entries.indices) {
            val e = entries.get(gi)
            val id = e.bookID
            val bookWithStatus = this.bookRegistry.books().get(id)
            if (bookWithStatus != null) {
              this.log.debug("updating entry {} from book registry", id)
              entries.set(gi, FeedEntry.FeedEntryOPDS(bookWithStatus.book().entry))
            }
          }
        }
      }
    }
  }

  companion object {

    /**
     * Create a new feed loader.
     */

    fun create(
      exec: ListeningExecutorService,
      parser: OPDSFeedParserType,
      searchParser: OPDSSearchParserType,
      transport: OPDSFeedTransportType<OptionType<HTTPAuthType>>,
      bookRegistry: BookRegistryReadableType,
      bundledContent: BundledContentResolverType): FeedLoaderType {

      val cache =
        ExpiringMap.builder()
          .expirationPolicy(ExpiringMap.ExpirationPolicy.CREATED)
          .expiration(5L, TimeUnit.MINUTES)
          .build<URI, Feed>()

      return FeedLoader(
        cache,
        exec,
        parser,
        searchParser,
        transport,
        bookRegistry,
        bundledContent)
    }
  }
}
