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
 * A feed view model.
 *
 * The responsibility of this class is essentially to make asynchronous calls to the feed loader
 * and profile API and convert those calls into observable events.
 */

class CatalogFeedViewModel(
  val context: Context,
  val services: ServiceDirectoryType,
  val feedArguments: CatalogFeedArguments
) : ViewModel(), CatalogFeedViewModelType {

  private val logger = LoggerFactory.getLogger(this.javaClass)

  private val feedLoader: FeedLoaderType =
    this.services.requireService(FeedLoaderType::class.java)
  private val configurationService: CatalogConfigurationServiceType =
    this.services.requireService(CatalogConfigurationServiceType::class.java)
  private val profilesController: ProfilesControllerType =
    this.services.requireService(ProfilesControllerType::class.java)
  private val instanceId =
    UUID.randomUUID()

  /**
   * The stack of feeds that lead to the current feed. The current feed is the feed on top
   * of this stack.
   */

  private val stateLock = Any()
  @GuardedBy("stateLock")
  private var state: CatalogFeedState? = null

  private fun loadFeed(
    arguments: CatalogFeedArguments
  ): CatalogFeedState {
    return when (arguments) {
      is CatalogFeedArgumentsRemote ->
        this.doLoadRemoteFeed(arguments)
      is CatalogFeedArgumentsLocalBooks ->
        this.doLoadLocalFeed(arguments)
    }
  }

  /**
   * Load a locally-generated feed.
   */

  private fun doLoadLocalFeed(
    arguments: CatalogFeedArgumentsLocalBooks
  ): CatalogFeedState {
    this.logger.debug("[{}]: loading local feed {}", this.instanceId, arguments.selection)

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

    return this.createNewStatus(arguments, future)
  }

  /**
   * Load a remote feed.
   */

  private fun doLoadRemoteFeed(
    arguments: CatalogFeedArgumentsRemote
  ): CatalogFeedState {
    this.logger.debug("[{}]: loading remote feed {}", instanceId, arguments.feedURI)

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

    return this.createNewStatus(arguments, future)
  }

  /**
   * Create a new feed state for the given operation. The feed is assumed to start in a "loading"
   * state.
   */

  private fun createNewStatus(
    arguments: CatalogFeedArguments,
    future: FluentFuture<FeedLoaderResult>
  ): CatalogFeedState.CatalogFeedLoading {
    val newState =
      CatalogFeedState.CatalogFeedLoading(
        arguments = arguments,
        future = future
      )

    synchronized(this.stateLock) {
      Preconditions.checkState(
        this.state == null,
        "State must be null (received ${this.state})"
      )
      this.state = newState
    }
    this.feedStatus.send(Unit)

    /*
     * Register a callback that updates the feed status when the future completes.
     */

    future.map { feedLoaderResult ->
      this.onFeedStatusUpdated(feedLoaderResult, newState)
      feedLoaderResult
    }
    return newState
  }

  private fun onFeedStatusUpdated(
    result: FeedLoaderResult,
    state: CatalogFeedState
  ) {
    this.logger.debug("[{}]: feed status updated: {}", result.javaClass)

    synchronized(this.stateLock) {
      this.state = this.feedLoaderResultToFeedState(result, state)
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
              arguments = state.arguments,
              feed = feed
            )
          is Feed.FeedWithGroups ->
            CatalogFeedWithGroups(
              arguments = state.arguments,
              feed = feed
            )
        }
      is FeedLoaderResult.FeedLoaderFailure ->
        CatalogFeedState.CatalogFeedLoadFailed(
          arguments = state.arguments,
          failure = result
        )
    }
  }

  override fun onCleared() {
    super.onCleared()
    this.logger.debug("[{}]: deleting viewmodel", this.instanceId)
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

  override val feedStatus: ObservableType<Unit> =
    Observable.create<Unit>()

  override fun feedState(): CatalogFeedState {
    val currentState = synchronized(this.stateLock, this::state)
    if (currentState != null) {
      return currentState
    }
    return this.loadFeed(this.feedArguments)
  }

  override fun resolveFeed(
    title: String,
    uri: URI,
    isSearchResults: Boolean
  ): CatalogFeedArguments {
    return when (val arguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote ->
        CatalogFeedArgumentsRemote(
          title = title,
          feedURI = arguments.feedURI.resolve(uri).normalize(),
          isSearchResults = isSearchResults
        )
      is CatalogFeedArgumentsLocalBooks ->
        CatalogFeedArgumentsRemote(
          title = title,
          feedURI = uri,
          isSearchResults = isSearchResults
        )
    }
  }
}
