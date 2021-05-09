package org.nypl.simplified.ui.catalog

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FilteringForAccount
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.futures.FluentFutureExtensions.flatMap
import org.nypl.simplified.futures.FluentFutureExtensions.fluentFutureOfAll
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.futures.FluentFutureExtensions.onAnyError
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty
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
  private val context: Context,
  private val services: ServiceDirectoryType,
  private val feedArguments: CatalogFeedArguments
) : ViewModel(), CatalogFeedViewModelType {

  private val logger = LoggerFactory.getLogger(this.javaClass)

  private val feedLoader: FeedLoaderType =
    this.services.requireService(FeedLoaderType::class.java)
  private val booksController: BooksControllerType =
    this.services.requireService(BooksControllerType::class.java)
  private val profilesController: ProfilesControllerType =
    this.services.requireService(ProfilesControllerType::class.java)
  private val buildConfig: BuildConfigurationServiceType =
    this.services.requireService(BuildConfigurationServiceType::class.java)
  private val instanceId =
    UUID.randomUUID()

  private var feedWithoutGroupsViewState: Parcelable? = null
  private var feedWithGroupsViewState: Parcelable? = null
  private var accountLoginSubscription: Disposable? = null

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

    val booksUri = URI.create("Books")

    val request =
      ProfileFeedRequest(
        facetTitleProvider = CatalogFacetPseudoTitleProvider(this.context.resources),
        feedSelection = arguments.selection,
        filterByAccountID = arguments.filterAccount,
        search = arguments.searchTerms,
        sortBy = arguments.sortBy,
        title = arguments.title,
        uri = booksUri
      )

    val profile =
      this.profilesController.profileCurrent()
    val accountsToSync =
      if (request.filterByAccountID == null) {
        // Sync all accounts
        profile.accounts()
      } else {
        // Sync the account we're filtering on
        profile.accounts().filterKeys { it == request.filterByAccountID }
      }

    val syncFuture =
      fluentFutureOfAll(
        accountsToSync.keys.map { account ->
          this.booksController.booksSync(account)
        }
      )

    val future = this.profilesController.profileFeed(request)
      .map { f -> FeedLoaderResult.FeedLoaderSuccess(f) as FeedLoaderResult }
      .onAnyError { ex -> FeedLoaderResult.wrapException(booksUri, ex) }

    return this.createNewStatus(
      arguments = arguments,
      future = syncFuture.flatMap { future }
    )
  }

  /**
   * Load a remote feed.
   */

  private fun doLoadRemoteFeed(
    arguments: CatalogFeedArgumentsRemote
  ): CatalogFeedState {
    this.logger.debug("[{}]: loading remote feed {}", this.instanceId, arguments.feedURI)

    val profile =
      this.profilesController.profileCurrent()
    val account =
      profile.account(arguments.ownership.accountId)

    /*
     * If the remote feed has an age gate, and we haven't given an age, then display an
     * age gate!
     */

    if (shouldDisplayAgeGate(account.provider.authentication, profile.preferences())) {
      this.logger.debug("[{}]: showing age gate", this.instanceId)
      val newState = CatalogFeedState.CatalogFeedAgeGate(this.feedArguments)
      synchronized(this.stateLock) {
        this.state = newState
      }
      this.feedStatusSource.onNext(Unit)
      return newState
    }

    val loginState =
      account.loginState
    val authentication =
      AccountAuthenticatedHTTP.createAuthorizationIfPresent(loginState.credentials)

    val future =
      this.feedLoader.fetchURIWithBookRegistryEntries(
        account = account.id,
        uri = arguments.feedURI,
        auth = authentication,
        method = "GET"
      )

    return this.createNewStatus(
      arguments = arguments,
      future = future
    )
  }

  private fun shouldDisplayAgeGate(
    authentication: AccountProviderAuthenticationDescription,
    preferences: ProfilePreferences
  ): Boolean {
    val isCoppa = authentication is AccountProviderAuthenticationDescription.COPPAAgeGate
    return isCoppa && buildConfig.showAgeGateUi && preferences.dateOfBirth == null
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
    /*
     * If the failure is due to bad credentials, then subscribe to events for the account
     * and try refreshing the feed when an account login has occurred.
     */

    if (result is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication) {
      when (val ownership = state.arguments.ownership) {
        is CatalogFeedOwnership.OwnedByAccount ->
          this.subscribeToLoginEvents(
            accountId = ownership.accountId,
            runOnSuccess = {
              this.logger.debug("reloading feed due to successful login")
              this.reloadFeed(state.arguments)
            }
          )
        CatalogFeedOwnership.CollectedFromAccounts -> {
          // We can't log in if we don't know which account owns the feed.
        }
      }
    }

    return CatalogFeedState.CatalogFeedLoadFailed(
      arguments = state.arguments,
      failure = result
    )
  }

  private fun subscribeToLoginEvents(
    accountId: AccountID,
    runOnSuccess: () -> Unit
  ) {
    this.accountLoginSubscription =
      this.profilesController.accountEvents()
        .ofType(AccountEventLoginStateChanged::class.java)
        .filter { event -> event.accountID == accountId }
        .subscribe { event ->
          when (event.state) {
            AccountLoginState.AccountNotLoggedIn,
            is AccountLoginState.AccountLoggingIn,
            is AccountLoginState.AccountLoggingInWaitingForExternalAuthentication,
            is AccountLoginState.AccountLoggingOut -> {
              // Still in progress!
            }

            is AccountLoginState.AccountLogoutFailed,
            is AccountLoginState.AccountLoginFailed -> {
              this.unsubscribeFromAccountEvents()
            }

            is AccountLoginState.AccountLoggedIn -> {
              this.unsubscribeFromAccountEvents()
              runOnSuccess()
            }
          }
        }
  }

  private fun unsubscribeFromAccountEvents() {
    this.accountLoginSubscription?.dispose()
    this.accountLoginSubscription = null
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
        ownership = this.feedArguments.ownership,
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
    this.unsubscribeFromAccountEvents()
  }

  private class CatalogFacetPseudoTitleProvider(
    val resources: Resources
  ) : FeedFacetPseudoTitleProviderType {
    override val sortByTitle: String
      get() = this.resources.getString(R.string.feedByTitle)
    override val sortByAuthor: String
      get() = this.resources.getString(R.string.feedByAuthor)
    override val collection: String
      get() = this.resources.getString(R.string.feedCollection)
    override val collectionAll: String
      get() = this.resources.getString(R.string.feedCollectionAll)
    override val sortBy: String
      get() = this.resources.getString(R.string.feedSortBy)
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
          feedURI = arguments.feedURI.resolve(uri).normalize(),
          isSearchResults = isSearchResults,
          ownership = arguments.ownership,
          title = title
        )

      is CatalogFeedArgumentsLocalBooks -> {
        throw IllegalStateException(
          "Can't transition local to remote feed: ${this.feedArguments.title} -> $title"
        )
      }
    }
  }

  override fun resolveFeedFromBook(
    accountID: AccountID,
    title: String,
    uri: URI
  ): CatalogFeedArguments {
    return when (val arguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote ->
        CatalogFeedArgumentsRemote(
          feedURI = arguments.feedURI.resolve(uri).normalize(),
          isSearchResults = false,
          ownership = CatalogFeedOwnership.OwnedByAccount(accountID),
          title = title
        )

      is CatalogFeedArgumentsLocalBooks -> {
        CatalogFeedArgumentsRemote(
          feedURI = uri.normalize(),
          isSearchResults = false,
          ownership = CatalogFeedOwnership.OwnedByAccount(accountID),
          title = title
        )
      }
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
              feedURI = currentArguments.feedURI.resolve(facet.opdsFacet.uri).normalize(),
              isSearchResults = currentArguments.isSearchResults,
              ownership = currentArguments.ownership,
              title = facet.title
            )

          is FeedFacetPseudo ->
            currentArguments
        }

      is CatalogFeedArgumentsLocalBooks -> {
        when (facet) {
          is FeedFacet.FeedFacetOPDS ->
            throw IllegalStateException("Cannot transition from a local feed to a remote feed.")

          is Sorting ->
            CatalogFeedArgumentsLocalBooks(
              filterAccount = currentArguments.filterAccount,
              ownership = currentArguments.ownership,
              searchTerms = currentArguments.searchTerms,
              selection = currentArguments.selection,
              sortBy = facet.sortBy,
              title = facet.title
            )

          is FilteringForAccount ->
            CatalogFeedArgumentsLocalBooks(
              filterAccount = facet.account,
              ownership = currentArguments.ownership,
              searchTerms = currentArguments.searchTerms,
              selection = currentArguments.selection,
              sortBy = currentArguments.sortBy,
              title = facet.title
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
              feedURI = currentArguments.feedURI,
              isSearchResults = true,
              ownership = currentArguments.ownership,
              title = currentArguments.title
            )
          }
          is FeedSearch.FeedSearchOpen1_1 -> {
            CatalogFeedArgumentsRemote(
              feedURI = search.search.getQueryURIForTerms(query),
              isSearchResults = true,
              ownership = currentArguments.ownership,
              title = currentArguments.title
            )
          }
        }
      }

      is CatalogFeedArgumentsLocalBooks -> {
        when (search) {
          FeedSearch.FeedSearchLocal -> {
            CatalogFeedArgumentsLocalBooks(
              filterAccount = currentArguments.filterAccount,
              ownership = currentArguments.ownership,
              searchTerms = query,
              selection = currentArguments.selection,
              sortBy = currentArguments.sortBy,
              title = currentArguments.title
            )
          }
          is FeedSearch.FeedSearchOpen1_1 -> {
            CatalogFeedArgumentsLocalBooks(
              filterAccount = currentArguments.filterAccount,
              ownership = currentArguments.ownership,
              searchTerms = query,
              selection = currentArguments.selection,
              sortBy = currentArguments.sortBy,
              title = currentArguments.title
            )
          }
        }
      }
    }
  }

  override fun updateBirthYear(over13: Boolean) {
    profilesController.profileUpdate { description ->
      val years = if (over13) 14 else 0
      this.synthesizeDateOfBirthDescription(description, years)
    }
  }

  private fun synthesizeDateOfBirthDescription(
    description: ProfileDescription,
    years: Int
  ): ProfileDescription {
    val newPreferences =
      description.preferences.copy(dateOfBirth = this.synthesizeDateOfBirth(years))
    return description.copy(preferences = newPreferences)
  }

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth {
    return ProfileDateOfBirth(
      date = DateTime.now().minusYears(years),
      isSynthesized = true
    )
  }
}
