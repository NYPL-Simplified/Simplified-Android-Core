package org.nypl.simplified.ui.catalog

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.Option
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.futures.FluentFutureExtensions.onAnyError
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.*
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
  private val context: Context,
  private val services: ServiceDirectoryType,
  private val feedArguments: CatalogFeedArguments
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

  private var feedWithoutGroupsViewState: Parcelable? = null
  private var feedWithGroupsViewState: Parcelable? = null

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
    this.logger.debug("[{}]: loading remote feed {}", this.instanceId, arguments.feedURI)

    val profile = this.profilesController.profileCurrent()
    val account = this.profilesController.profileAccountCurrent()

    /*
     * If the remote feed has an age gate, and we haven't given an age, then display an
     * age gate!
     */

    when (account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> {
        val dateOfBirth = profile.preferences().dateOfBirth
        if (dateOfBirth == null) {
          this.logger.debug("[{}]: showing age gate", this.instanceId)
          val newState = CatalogFeedState.CatalogFeedAgeGate(this.feedArguments)
          synchronized(this.stateLock) {
            this.state = newState
          }
          this.feedStatusSource.onNext(Unit)
          return newState
        }
      }
      is AccountProviderAuthenticationDescription.Basic,
      null -> {

      }
    }

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
      this.state = newState
    }
    this.feedStatusSource.onNext(Unit)

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
    this.logger.debug("[{}]: feed status updated: {}", this.instanceId, result.javaClass)

    synchronized(this.stateLock) {
      this.state = this.feedLoaderResultToFeedState(result, state)
    }

    this.feedStatusSource.onNext(Unit)
  }

  private fun feedLoaderResultToFeedState(
    result: FeedLoaderResult,
    state: CatalogFeedState
  ): CatalogFeedState {
    return when (result) {
      is FeedLoaderResult.FeedLoaderSuccess ->
        when (val feed = result.feed) {
          is Feed.FeedWithoutGroups ->
            this.onReceivedFeedWithoutGroups(state, feed)
          is Feed.FeedWithGroups ->
            this.onReceivedFeedWithGroups(state, feed)
        }
      is FeedLoaderResult.FeedLoaderFailure ->
        this.onReceivedFeedFailure(state, result)
    }
  }

  private fun onReceivedFeedFailure(
    state: CatalogFeedState,
    result: FeedLoaderResult.FeedLoaderFailure
  ): CatalogFeedState.CatalogFeedLoadFailed {
    return CatalogFeedState.CatalogFeedLoadFailed(
      arguments = state.arguments,
      failure = result
    )
  }

  private fun onReceivedFeedWithGroups(
    state: CatalogFeedState,
    feed: Feed.FeedWithGroups
  ): CatalogFeedLoaded {
    if (feed.size == 0) {
      return CatalogFeedEmpty(
        arguments = state.arguments,
        search = feed.feedSearch,
        title = feed.feedTitle
      )
    }

    return CatalogFeedWithGroups(
      arguments = state.arguments,
      feed = feed
    )
  }

  private fun onReceivedFeedWithoutGroups(
    state: CatalogFeedState,
    feed: Feed.FeedWithoutGroups
  ): CatalogFeedLoaded {

    if (feed.entriesInOrder.isEmpty()) {
      return CatalogFeedEmpty(
        arguments = state.arguments,
        search = feed.feedSearch,
        title = feed.feedTitle
      )
    }

    /*
     * Construct a paged list for infinitely scrolling feeds.
     */

    val dataSourceFactory =
      CatalogPagedDataSourceFactory(
        feedLoader = this.feedLoader,
        initialFeed = feed,
        profilesController = this.profilesController
      )

    val pagedListConfig =
      PagedList.Config.Builder()
        .setEnablePlaceholders(true)
        .setPageSize(50)
        .setMaxSize(250)
        .setPrefetchDistance(25)
        .build()

    val pagedList =
      LivePagedListBuilder(dataSourceFactory, pagedListConfig)
        .build()

    return CatalogFeedWithoutGroups(
      arguments = state.arguments,
      entries = pagedList,
      facetsInOrder = feed.facetsOrder,
      facetsByGroup = feed.facetsByGroup,
      search = feed.feedSearch,
      title = feed.feedTitle
    )
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

  private val feedStatusSource =
    PublishSubject.create<Unit>()

  override val feedStatus: Observable<Unit> =
    this.feedStatusSource

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
          isSearchResults = isSearchResults,
          accountId = arguments.accountId
        )
      is CatalogFeedArgumentsLocalBooks ->
        CatalogFeedArgumentsRemote(
          title = title,
          feedURI = uri,
          isSearchResults = isSearchResults,
          accountId = arguments.accountId
        )
    }
  }

  override fun reloadFeed(arguments: CatalogFeedArguments) {
    synchronized(this.stateLock) {
      this.state = null
    }
    this.loadFeed(arguments)
  }

  override fun resolveFacet(
    facet: FeedFacet
  ): CatalogFeedArguments {
    return when (val currentArguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote ->
        when (facet) {
          is FeedFacet.FeedFacetOPDS ->
            CatalogFeedArgumentsRemote(
              title = facet.title,
              feedURI = currentArguments.feedURI.resolve(facet.opdsFacet.uri).normalize(),
              isSearchResults = currentArguments.isSearchResults,
              accountId = currentArguments.accountId
            )
          is FeedFacet.FeedFacetPseudo ->
            currentArguments
        }
      is CatalogFeedArgumentsLocalBooks -> {
        when (facet) {
          is FeedFacet.FeedFacetOPDS ->
            CatalogFeedArgumentsRemote(
              title = facet.title,
              feedURI = facet.opdsFacet.uri,
              isSearchResults = currentArguments.isSearchResults,
              accountId = currentArguments.accountId
            )
          is FeedFacet.FeedFacetPseudo ->
            CatalogFeedArgumentsLocalBooks(
              title = facet.title,
              facetType = facet.type,
              selection = currentArguments.selection,
              searchTerms = currentArguments.searchTerms,
              accountId = currentArguments.accountId
            )
        }
      }
    }
  }

  override fun saveFeedWithGroupsViewState(state: Parcelable?) {
    this.feedWithGroupsViewState = state
  }

  override fun restoreFeedWithGroupsViewState(): Parcelable? {
    return this.feedWithGroupsViewState
  }

  override fun saveFeedWithoutGroupsViewState(state: Parcelable?) {
    this.feedWithoutGroupsViewState = state
  }

  override fun restoreFeedWithoutGroupsViewState(): Parcelable? {
    return this.feedWithoutGroupsViewState
  }

  override fun resolveSearch(
    search: FeedSearch,
    query: String
  ): CatalogFeedArguments {
    return when (val currentArguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote -> {
        when (search) {
          FeedSearch.FeedSearchLocal -> {
            CatalogFeedArgumentsRemote(
              title = currentArguments.title,
              feedURI = currentArguments.feedURI,
              isSearchResults = true,
              accountId = currentArguments.accountId
            )
          }
          is FeedSearch.FeedSearchOpen1_1 -> {
            CatalogFeedArgumentsRemote(
              title = currentArguments.title,
              feedURI = search.search.getQueryURIForTerms(query),
              isSearchResults = true,
              accountId = currentArguments.accountId
            )
          }
        }
      }
      is CatalogFeedArgumentsLocalBooks -> {
        when (search) {
          FeedSearch.FeedSearchLocal -> {
            CatalogFeedArgumentsLocalBooks(
              title = currentArguments.title,
              facetType = currentArguments.facetType,
              searchTerms = query,
              selection = currentArguments.selection,
              accountId = currentArguments.accountId
            )
          }
          is FeedSearch.FeedSearchOpen1_1 -> {
            CatalogFeedArgumentsLocalBooks(
              title = currentArguments.title,
              facetType = currentArguments.facetType,
              searchTerms = query,
              selection = currentArguments.selection,
              accountId = currentArguments.accountId
            )
          }
        }
      }
    }
  }
}
