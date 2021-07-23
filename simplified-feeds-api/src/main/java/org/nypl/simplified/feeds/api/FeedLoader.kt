package org.nypl.simplified.feeds.api

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.Some
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.bundled.api.BundledURIs
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPaths
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import org.nypl.simplified.opds.core.OPDSSearchLink
import org.nypl.simplified.opds.core.OPDSSearchParserType
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.net.URI
import java.util.SortedMap
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The default implementation of the [FeedLoaderType] interface.
 */

class FeedLoader private constructor(
  private val bookFormatSupport: BookFormatSupportType,
  private val bundledContent: BundledContentResolverType,
  private val contentResolver: ContentResolverType,
  private val exec: ListeningExecutorService,
  private val parser: OPDSFeedParserType,
  private val searchParser: OPDSSearchParserType,
  private val transport: OPDSFeedTransportType<LSHTTPAuthorizationType?>
) : FeedLoaderType {

  private val log = LoggerFactory.getLogger(FeedLoader::class.java)

  private val filterFlag =
    AtomicBoolean(true)

  override var showOnlySupportedBooks: Boolean
    get() = this.filterFlag.get()
    set(value) {
      this.filterFlag.set(value)
    }

  override fun fetchURI(
    account: AccountID,
    uri: URI,
    auth: LSHTTPAuthorizationType?,
    method: String,
  ): FluentFuture<FeedLoaderResult> {
    return FluentFuture.from(
      this.exec.submit(
        Callable {
          this.fetchSynchronously(
            accountId = account,
            uri = uri,
            auth = auth,
            method = method
          )
        }
      )
    )
  }

  private fun fetchSynchronously(
    accountId: AccountID,
    uri: URI,
    auth: LSHTTPAuthorizationType?,
    method: String
  ): FeedLoaderResult {
    try {
      /*
       * If the URI has a scheme that refers to bundled content, fetch the data from
       * the resolver instead.
       */

      if (BundledURIs.isBundledURI(uri)) {
        return this.parseFromBundledContent(accountId, uri)
      }

      /*
       * If the URI has a scheme that indicates that it must go through the Android
       * content resolver... do that.
       */

      if (uri.scheme == "content") {
        return this.parseFromContentResolver(accountId, uri)
      }

      /*
       * Otherwise, parse the OPDS feed including any embedded search links.
       */

      val opdsFeed =
        this.transport.getStream(auth, uri, method).use { stream -> this.parser.parse(uri, stream) }
      val search =
        this.fetchSearchLink(opdsFeed, auth, method)
      val feed =
        Feed.fromAcquisitionFeed(
          accountId = accountId,
          feed = opdsFeed,
          filter = this::isEntrySupported,
          search = search
        )

      return FeedLoaderSuccess(feed)
    } catch (e: FeedHTTPTransportException) {
      this.log.error("feed transport exception: ", e)

      if (e.code == 401) {
        return FeedLoaderFailure.FeedLoaderFailedAuthentication(
          problemReport = e.report,
          exception = e,
          attributesInitial = this.errorAttributesOf(uri, method),
          message = e.localizedMessage ?: ""
        )
      }
      return FeedLoaderFailure.FeedLoaderFailedGeneral(
        problemReport = e.report,
        exception = e,
        attributesInitial = this.errorAttributesOf(uri, method),
        message = e.localizedMessage ?: ""
      )
    } catch (e: Exception) {
      this.log.error("feed exception: ", e)

      return FeedLoaderFailure.FeedLoaderFailedGeneral(
        problemReport = null,
        exception = e,
        attributesInitial = this.errorAttributesOf(uri, method),
        message = e.localizedMessage ?: ""
      )
    }
  }

  private fun isEntrySupported(
    entry: OPDSAcquisitionFeedEntry
  ): Boolean {
    if (!this.showOnlySupportedBooks) {
      return true
    }

    val linearizedPaths = OPDSAcquisitionPaths.linearize(entry)
    for (path in linearizedPaths) {
      val relationSupported = this.isRelationSupported(path.source.relation)
      val typePathSupported = this.isTypePathSupported(path)
      if (relationSupported && typePathSupported) {
        return true
      }
    }
    return false
  }

  private fun isRelationSupported(relation: OPDSAcquisition.Relation): Boolean =
    when (relation) {
      ACQUISITION_BORROW -> true
      ACQUISITION_BUY -> false
      ACQUISITION_GENERIC -> true
      ACQUISITION_OPEN_ACCESS -> true
      ACQUISITION_SAMPLE -> false
      ACQUISITION_SUBSCRIBE -> false
    }

  private fun isTypePathSupported(path: OPDSAcquisitionPath): Boolean {
    return this.bookFormatSupport.isSupportedPath(path.asMIMETypes())
  }

  private fun parseFromContentResolver(
    accountId: AccountID,
    uri: URI
  ): FeedLoaderResult {
    val streamMaybe = this.contentResolver.openInputStream(uri)
    return if (streamMaybe != null) {
      streamMaybe.use { stream ->
        FeedLoaderSuccess(
          Feed.fromAcquisitionFeed(
            accountId = accountId,
            feed = this.parser.parse(uri, stream),
            search = null,
            filter = this::isEntrySupported
          )
        )
      }
    } else {
      FeedLoaderFailure.FeedLoaderFailedGeneral(
        problemReport = null,
        exception = FileNotFoundException(),
        message = "File not found!",
        attributesInitial = mapOf(
          Pair("URI", uri.toASCIIString())
        )
      )
    }
  }

  private fun errorAttributesOf(
    uri: URI,
    method: String
  ): SortedMap<String, String> {
    return sortedMapOf(
      Pair("Feed", uri.toString()),
      Pair("Method", method)
    )
  }

  private fun parseFromBundledContent(
    accountId: AccountID,
    uri: URI
  ): FeedLoaderSuccess {
    return this.bundledContent.resolve(uri).use { stream ->
      FeedLoaderSuccess(
        Feed.fromAcquisitionFeed(
          accountId = accountId,
          feed = this.parser.parse(uri, stream),
          filter = this::isEntrySupported,
          search = null
        )
      )
    }
  }

  private fun fetchSearchLink(
    opdsFeed: OPDSAcquisitionFeed,
    auth: LSHTTPAuthorizationType?,
    method: String
  ): OPDSOpenSearch1_1? {
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

  companion object {

    /**
     * Create a new feed loader.
     */

    fun create(
      bookFormatSupport: BookFormatSupportType,
      contentResolver: ContentResolverType,
      exec: ListeningExecutorService,
      parser: OPDSFeedParserType,
      searchParser: OPDSSearchParserType,
      transport: OPDSFeedTransportType<LSHTTPAuthorizationType?>,
      bundledContent: BundledContentResolverType
    ): FeedLoaderType {
      return FeedLoader(
        bookFormatSupport = bookFormatSupport,
        bundledContent = bundledContent,
        contentResolver = contentResolver,
        exec = exec,
        parser = parser,
        searchParser = searchParser,
        transport = transport
      )
    }
  }
}
