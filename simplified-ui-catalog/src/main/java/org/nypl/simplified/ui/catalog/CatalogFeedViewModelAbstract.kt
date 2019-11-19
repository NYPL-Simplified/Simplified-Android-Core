package org.nypl.simplified.ui.catalog

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.Option
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.futures.FluentFutureExtensions.onAnyError
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import javax.annotation.concurrent.GuardedBy

/**
 * The base type of catalog feed view models. This class is abstract purely because the AndroidX
 * ViewModel API requires that we fetch view models by class, and we need to store separate view
 * models for each of the different app sections that want to display feeds.
 *
 * The responsibilities of this class are essentially to make asynchronous calls to the feed loader
 * and profile API and convert those calls into observable events, and to manage a history in the
 * form of a stack of feed state values.
 *
 * @see [CatalogFeedViewModelBooks]
 * @see [CatalogFeedViewModelHolds]
 * @see [CatalogFeedViewModelExternal]
 */

abstract class CatalogFeedViewModelAbstract(
  val context: Context,
  val services: ServiceDirectoryType
) : ViewModel(), CatalogFeedViewModelType {

  private val logger = LoggerFactory.getLogger(this.javaClass)

  private val feedLoader: FeedLoaderType =
    this.services.requireService(FeedLoaderType::class.java)
  private val configurationService: CatalogConfigurationServiceType =
    this.services.requireService(CatalogConfigurationServiceType::class.java)
  private val profilesController: ProfilesControllerType =
    this.services.requireService(ProfilesControllerType::class.java)

  /**
   * The stack of feeds that lead to the current feed. The current feed is the feed on top
   * of this stack.
   */

  private val historyLock = Any()
  @GuardedBy("historyLock")
  private var history: List<CatalogFeedState> = listOf()

  /**
   * The initial feed URI that will be used for the feed view.
   */

  abstract fun initialFeedArguments(
    context: Context,
    profiles: ProfilesControllerType
  ): CatalogFeedArguments

  private fun loadFeed(
    requestId: UUID,
    arguments: CatalogFeedArguments
  ): CatalogFeedState {
    return when (arguments) {
      is CatalogFeedArgumentsRemote ->
        this.doLoadRemoteFeed(requestId, arguments)
      is CatalogFeedArgumentsLocalBooks ->
        this.doLoadLocalFeed(requestId, arguments)
    }
  }

  /**
   * Load a locally-generated feed.
   */

  private fun doLoadLocalFeed(
    requestId: UUID,
    arguments: CatalogFeedArgumentsLocalBooks
  ): CatalogFeedState {
    this.logger.debug("[{}]: loading local feed {}", requestId, arguments.selection)

    val booksUri =
      URI.create("Books")

    val showAllCollections =
      this.configurationService.showAllCollectionsInLocalFeeds

    val filterAccountID =
      if (!showAllCollections) {
        this.profilesController.profileAccountCurrent().id
      } else {
        null
      }

    val request =
      ProfileFeedRequest(
        facetActive = arguments.facetType,
        facetGroup = this.context.getString(R.string.feedSortBy),
        facetTitleProvider = CatalogFacetPseudoTitleProvider(this.context.resources),
        feedSelection = arguments.selection,
        filterByAccountID = filterAccountID,
        search = arguments.searchTerms,
        title = this.context.getString(R.string.feedTitleBooks),
        uri = booksUri)

    val future =
      this.profilesController.profileFeed(request)
        .map { f -> FeedLoaderResult.FeedLoaderSuccess(f) as FeedLoaderResult }
        .onAnyError { ex -> FeedLoaderResult.wrapException(booksUri, ex) }

    return this.createNewStatus(requestId, arguments, future)
  }

  /**
   * Load a remote feed.
   */

  private fun doLoadRemoteFeed(
    requestId: UUID,
    arguments: CatalogFeedArgumentsRemote
  ): CatalogFeedState {
    this.logger.debug("[{}]: loading remote feed {}", requestId, arguments.feedURI)

    val account = this.profilesController.profileAccountCurrent()
    val loginState = account.loginState
    val authentication =
      if (loginState.credentials != null) {
        Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(loginState.credentials))
      } else {
        Option.none()
      }

    val future =
      this.feedLoader.fetchURIWithBookRegistryEntries(arguments.feedURI, authentication)

    return this.createNewStatus(requestId, arguments, future)
  }

  /**
   * Create a new feed state for the given operation. The feed is assumed to start in a "loading"
   * state.
   */

  private fun createNewStatus(
    requestId: UUID,
    arguments: CatalogFeedArguments,
    future: FluentFuture<FeedLoaderResult>
  ): CatalogFeedState.CatalogFeedLoading {
    val newRequestState =
      CatalogFeedState.CatalogFeedLoading(
        requestId = requestId,
        arguments = arguments,
        future = future
      )

    synchronized(this.historyLock) {
      Preconditions.checkState(
        !this.history.any { state -> state.requestId == requestId },
        "There must not be an existing state with request ID $requestId")

      this.history = this.history.plus(newRequestState)
    }
    this.feedStatus.send(Unit)

    /*
     * Register a callback that updates the feed status when the future completes.
     */

    future.map { feedLoaderResult ->
      this.onFeedStatusUpdated(requestId, feedLoaderResult)
      feedLoaderResult
    }
    return newRequestState
  }

  private fun onFeedStatusUpdated(
    requestId: UUID,
    result: FeedLoaderResult
  ) {
    this.logger.debug("[{}]: feed status updated: {}", requestId, result.javaClass)

    synchronized(this.historyLock) {
      this.history = this.history.map { state ->
        if (state.requestId == requestId) {
          this.feedLoaderResultToFeedState(result, state)
        } else {
          state
        }
      }
    }

    this.feedStatus.send(Unit)
  }

  private fun feedLoaderResultToFeedState(
    result: FeedLoaderResult,
    state: CatalogFeedState
  ): CatalogFeedState {
    return when (result) {
      is FeedLoaderResult.FeedLoaderSuccess ->
        when (val feed = result.feed) {
          is Feed.FeedWithoutGroups ->
            CatalogFeedWithoutGroups(
              requestId = state.requestId,
              arguments = state.arguments,
              feed = feed
            )
          is Feed.FeedWithGroups ->
            CatalogFeedWithGroups(
              requestId = state.requestId,
              arguments = state.arguments,
              feed = feed
            )
        }
      is FeedLoaderResult.FeedLoaderFailure ->
        CatalogFeedState.CatalogFeedLoadFailed(
          requestId = state.requestId,
          arguments = state.arguments,
          failure = result
        )
    }
  }

  /**
   * Attempt to resolve a URI.
   */

  private fun resolveURI(uri: URI): URI {
    if (uri.isAbsolute) {
      return uri
    }

    val currentState = synchronized(this.historyLock) {
      this.history.lastOrNull()
    } ?: return uri

    return when (val arguments = currentState.arguments) {
      is CatalogFeedArgumentsRemote ->
        arguments.feedURI.resolve(uri).normalize()
      is CatalogFeedArgumentsLocalBooks ->
        uri
    }
  }

  private class CatalogFacetPseudoTitleProvider(
    val resources: Resources
  ) : FeedFacetPseudoTitleProviderType {
    override fun getTitle(t: FeedFacet.FeedFacetPseudo.FacetType): String {
      return when (t) {
        FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_AUTHOR ->
          this.resources.getString(R.string.feedByAuthor)
        FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE ->
          this.resources.getString(R.string.feedByTitle)
      }
    }
  }

  /**
   * The status of the current feed.
   */

  override val feedStatus: ObservableType<Unit> =
    Observable.create<Unit>()

  /**
   * Retrieve the status of the current feed, or load a new feed using the initial feed
   * arguments defined for this view model.
   *
   * @see [initialFeedArguments]
   */

  override fun feedState(): CatalogFeedState {
    val currentState = synchronized(this.historyLock) {
      this.history.lastOrNull()
    }

    if (currentState != null) {
      return currentState
    }

    return this.loadFeed(
      requestId = UUID.randomUUID(),
      arguments = this.initialFeedArguments(this.context, this.profilesController)
    )
  }

  /**
   * Resolve and load a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the URI at the top of the current request stack (assuming that the top of the stack refers
   * to a remote feed).
   *
   * @param title The title of the feed
   * @param uri The URI of the remote feed
   * @param isSearchResults `true` if the feed refers to search results
   */

  override fun resolveAndLoadFeed(
    title: String,
    uri: URI,
    isSearchResults: Boolean
  ): CatalogFeedState {
    val requestId = UUID.randomUUID()
    this.logger.debug("[{}]: resolving and loading feed: {}", requestId, uri)
    val resolvedURI = this.resolveURI(uri)
    this.logger.debug("[{}]: resolved URI: {}", requestId, resolvedURI)

    return this.loadFeed(
      requestId = requestId,
      arguments = CatalogFeedArgumentsRemote(
        title = title,
        feedURI = resolvedURI,
        isSearchResults = isSearchResults
      ))
  }
}
