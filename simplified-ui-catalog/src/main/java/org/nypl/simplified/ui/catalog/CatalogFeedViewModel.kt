package org.nypl.simplified.ui.catalog

import android.content.Context
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LegacyPagingSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FilteringForAccount
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.futures.FluentFutureExtensions.onAnyError
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem
import org.nypl.simplified.ui.catalog.withoutGroups.DownloadState
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIExecutor
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import javax.annotation.concurrent.GuardedBy

/**
 * A view model for the catalog feed fragment.
 */

class CatalogFeedViewModel(
  private val resources: Resources,
  private val profilesController: ProfilesControllerType,
  private val feedLoader: FeedLoaderType,
  private val bookRegistry: BookRegistryType,
  private val buildConfiguration: BuildConfigurationServiceType,
  private val analytics: AnalyticsType,
  private val borrowViewModel: CatalogBorrowViewModel,
  private val feedArguments: CatalogFeedArguments,
  private val listener: FragmentListenerType<CatalogFeedEvent>,
  private val uiExecutor: UIExecutor,
  private val pagingFetchDispatcher: CoroutineDispatcher = Dispatchers.IO,
  doInitialLoad: Boolean = true // Adding this temporarily to enable easier testing
) : ViewModel(), CatalogPagedViewListener {

  private val instanceId =
    UUID.randomUUID()

  private val logger =
    LoggerFactory.getLogger(javaClass)

  private val stateMutable: MutableLiveData<CatalogFeedState> =
    MutableLiveData(CatalogFeedState.CatalogFeedLoading(feedArguments))

  init {
    if (doInitialLoad) loadFeed(feedArguments)
  }

  private val state: CatalogFeedState
    get() = feedStateLiveData.value!!

  private data class LoaderResultWithArguments(
    val arguments: CatalogFeedArguments,
    val result: FeedLoaderResult
  )

  @GuardedBy("loaderResults")
  private val loaderResults =
    PublishSubject.create<LoaderResultWithArguments>()

  private val subscriptions =
    CompositeDisposable(
      profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(::onAccountEvent),
      bookRegistry.bookEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(::onBookStatusEvent),
      loaderResults
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(::onFeedLoaderResult)
    )

  private val downloadingBooks = mutableMapOf<BookID, BookStatus>()

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded -> {
        if (state.arguments.isLocallyGenerated) {
          reloadFeed()
        }
      }
      is AccountEventLoginStateChanged ->
        onLoginStateChanged(event.accountID, event.state)
    }
  }

  private fun onLoginStateChanged(accountID: AccountID, accountState: AccountLoginState) {
    val feedState = state

    when (val ownership = feedState.arguments.ownership) {
      is CatalogFeedOwnership.OwnedByAccount -> {
        /*
         * If loading the feed failed due to bad credentials and an account login has occurred,
         * try refreshing the feed.
         */

        if (
          accountState is AccountLoginState.AccountLoggedIn &&
          ownership.accountId == accountID &&
          feedState is CatalogFeedState.CatalogFeedLoadFailed &&
          feedState.failure is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
        ) {
          logger.debug("reloading feed due to successful login")
          reloadFeed()
        }
      }
      CatalogFeedOwnership.CollectedFromAccounts -> {
        if (
          accountState is AccountLoginState.AccountLoggedIn ||
          accountState is AccountLoginState.AccountNotLoggedIn
        ) {
          logger.debug("reloading feed due to successful login or logout")
          reloadFeed()
        }
      }
    }
  }

  private fun onAgeUpdateCompleted(result: ProfileUpdated) {
    when (result) {
      is ProfileUpdated.Succeeded -> {
        when (val ownership = state.arguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> {
            val ageChanged =
              result.newDescription.preferences.dateOfBirth != result.oldDescription.preferences.dateOfBirth
            if (ageChanged) {
              val account = profilesController.profileCurrent().account(ownership.accountId)
              onAgeUpdateSuccess(account, ownership, result)
            }
          }
        }
      }
    }
  }

  private fun onAgeUpdateSuccess(
    account: AccountReadableType,
    ownership: CatalogFeedOwnership.OwnedByAccount,
    result: ProfileUpdated.Succeeded
  ) {
    val now = DateTime.now()
    val oldAge = result.oldDescription.preferences.dateOfBirth?.yearsOld(now)
    val newAge = result.newDescription.preferences.dateOfBirth?.yearsOld(now)
    logger.debug("age updated from {} to {}", oldAge, newAge)

    newAge?.let { age ->
      val newParameters = CatalogFeedArgumentsRemote(
        title = state.arguments.title,
        ownership = ownership,
        feedURI = account.catalogURIForAge(age),
        isSearchResults = false
      )
      loadFeed(newParameters)
    }
  }

  private fun onBookStatusEvent(event: BookStatusEvent) {
    /*
    Re-emit the existing state, which will cause the observing CatalogFragment to 'reconfigureUI'
    and resubscribe to the book items Pager flow ensuring a fresh emission with updated book statuses.
    Not a great setup but a temporarily working one until additional refactoring work is done in here.

    Only emit this value if in a WithoutGroups state because the VM is always subscribed to
    book events, but this is the only state that uses these events to drive a UI state change

    FeedArguments are never locally generated in current builds of the app.
     */
    if (stateMutable.hasActiveObservers() &&
      stateMutable.value is CatalogFeedWithoutGroups
    ) stateMutable.value = stateMutable.value

    when (event.statusNow) {
      is BookStatus.Held, is BookStatus.Loaned, is BookStatus.Revoked -> {
        if (state.arguments.isLocallyGenerated) {
          reloadFeed()
        }
      }
    }
  }

  private fun synthesizeBookWithStatus(
    item: FeedEntry.FeedEntryOPDS
  ): BookWithStatus {
    val book = Book(
      id = item.bookID,
      account = item.accountID,
      cover = null,
      thumbnail = null,
      entry = item.feedEntry,
      formats = listOf()
    )
    val status = BookStatus.fromBook(book)
    logger.debug("Synthesizing {} with status {}", book.id, status)
    return BookWithStatus(book, status)
  }

  override fun onCleared() {
    super.onCleared()
    logger.debug("[{}]: deleting viewmodel", instanceId)
    subscriptions.clear()
    uiExecutor.dispose()
  }

  val feedStateLiveData: LiveData<CatalogFeedState>
    get() = stateMutable

  fun reloadFeed() {
    loadFeed(state.arguments)
  }

  private fun loadFeed(
    arguments: CatalogFeedArguments
  ) {
    return when (arguments) {
      is CatalogFeedArgumentsRemote ->
        doLoadRemoteFeed(arguments)
      is CatalogFeedArgumentsLocalBooks ->
        doLoadLocalFeed(arguments)
    }
  }

  /**
   * Load a locally-generated feed.
   */

  private fun doLoadLocalFeed(
    arguments: CatalogFeedArgumentsLocalBooks
  ) {
    logger.debug("[{}]: loading local feed {}", instanceId, arguments.selection)

    val booksUri = URI.create("Books")

    val request =
      ProfileFeedRequest(
        facetTitleProvider = CatalogFacetPseudoTitleProvider(resources),
        feedSelection = arguments.selection,
        filterByAccountID = arguments.filterAccount,
        search = arguments.searchTerms,
        sortBy = arguments.sortBy,
        title = arguments.title,
        uri = booksUri
      )

    val future = profilesController.profileFeed(request)
      .map { f -> FeedLoaderResult.FeedLoaderSuccess(f) as FeedLoaderResult }
      .onAnyError { ex -> FeedLoaderResult.wrapException(booksUri, ex) }

    createNewStatus(
      arguments = arguments,
      future = future
    )
  }

  /**
   * Load a remote feed.
   */

  private fun doLoadRemoteFeed(
    arguments: CatalogFeedArgumentsRemote
  ) {
    logger.debug("[{}]: loading remote feed {}", instanceId, arguments.feedURI)

    val profile =
      profilesController.profileCurrent()
    val account =
      profile.account(arguments.ownership.accountId)

    /*
     * If the remote feed has an age gate, and we haven't given an age, then display an
     * age gate!
     */

    if (shouldDisplayAgeGate(account.provider.authentication, profile.preferences())) {
      logger.debug("[{}]: showing age gate", instanceId)
      val newState = CatalogFeedState.CatalogFeedAgeGate(arguments)
      stateMutable.value = newState
      return
    }

    val future =
      feedLoader.fetchURI(
        account = account,
        uri = arguments.feedURI,
        method = "GET"
      )

    createNewStatus(
      arguments = arguments,
      future = future
    )
  }

  private fun shouldDisplayAgeGate(
    authentication: AccountProviderAuthenticationDescription,
    preferences: ProfilePreferences
  ): Boolean {
    val isCoppa = authentication is AccountProviderAuthenticationDescription.COPPAAgeGate
    return isCoppa && buildConfiguration.showAgeGateUi && preferences.dateOfBirth == null
  }

  /**
   * Create a new feed state for the given operation. The feed is assumed to start in a "loading"
   * state.
   */

  private fun createNewStatus(
    arguments: CatalogFeedArguments,
    future: FluentFuture<FeedLoaderResult>
  ) {
    val newState =
      CatalogFeedState.CatalogFeedLoading(arguments)

    stateMutable.value = newState

    /*
     * Register a callback that updates the feed status when the future completes.
     */

    future.map { feedLoaderResult ->
      synchronized(loaderResults) {
        val resultWithArguments = LoaderResultWithArguments(arguments, feedLoaderResult)
        loaderResults.onNext(resultWithArguments)
      }
    }
  }

  private fun onFeedLoaderResult(resultWithArguments: LoaderResultWithArguments) {
    onFeedStatusUpdated(resultWithArguments.result, resultWithArguments.arguments)
  }

  private fun onFeedStatusUpdated(
    result: FeedLoaderResult,
    arguments: CatalogFeedArguments
  ) {
    logger.debug("[{}]: feed status updated: {}", instanceId, result.javaClass)

    stateMutable.value = feedLoaderResultToFeedState(arguments, result)
  }

  private fun feedLoaderResultToFeedState(
    arguments: CatalogFeedArguments,
    result: FeedLoaderResult
  ): CatalogFeedState {
    return when (result) {
      is FeedLoaderResult.FeedLoaderSuccess ->
        when (val feed = result.feed) {
          is Feed.FeedWithoutGroups ->
            onReceivedFeedWithoutGroups(arguments, feed)
          is Feed.FeedWithGroups ->
            onReceivedFeedWithGroups(arguments, feed)
        }
      is FeedLoaderResult.FeedLoaderFailure ->
        onReceivedFeedFailure(arguments, result)
    }
  }

  private fun onReceivedFeedFailure(
    arguments: CatalogFeedArguments,
    result: FeedLoaderResult.FeedLoaderFailure
  ): CatalogFeedState.CatalogFeedLoadFailed {
    /*
    * If the feed can't be loaded due to an authentication failure, then open
    * the account screen (if possible).
    */

    when (result) {
      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral -> {
        // Display the error.
      }
      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication -> {
        when (val ownership = state.arguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> {
            val shouldAuthenticate =
              profilesController.profileCurrent()
                .account(ownership.accountId)
                .requiresCredentials

            if (shouldAuthenticate) {
              /*
               * Explicitly deferring the opening of the fragment is required due to the
               * tabbed navigation controller eagerly instantiating fragments and causing
               * fragment transaction exceptions. This will go away when we have a replacement
               * for the navigator library.
               */

              listener.post(CatalogFeedEvent.LoginRequired(ownership.accountId))
            }
          }
          CatalogFeedOwnership.CollectedFromAccounts -> {
            // Nothing we can do here! We don't know which account owns the feed.
          }
        }
      }
    }

    return CatalogFeedState.CatalogFeedLoadFailed(
      arguments = arguments,
      failure = result
    )
  }

  private fun onReceivedFeedWithGroups(
    arguments: CatalogFeedArguments,
    feed: Feed.FeedWithGroups
  ): CatalogFeedLoaded {
    if (feed.size == 0) {
      return CatalogFeedEmpty(
        arguments = arguments,
        search = feed.feedSearch,
        title = feed.feedTitle
      )
    }

    return CatalogFeedWithGroups(
      arguments = arguments,
      feed = feed
    )
  }

  private fun onReceivedFeedWithoutGroups(
    arguments: CatalogFeedArguments,
    feed: Feed.FeedWithoutGroups
  ): CatalogFeedLoaded {
    if (feed.entriesInOrder.isEmpty()) {
      return CatalogFeedEmpty(
        arguments = arguments,
        search = feed.feedSearch,
        title = feed.feedTitle
      )
    }

    /*
     * Construct a paged list for infinitely scrolling feeds.
     */

    val dataSourceFactory =
      CatalogPagedDataSourceFactory(
        feedLoader = feedLoader,
        initialFeed = feed,
        ownership = feedArguments.ownership,
        profilesController = profilesController
      )

    val pagingConfig = PagingConfig(
      pageSize = 50,
      maxSize = 250,
      prefetchDistance = 25,
      enablePlaceholders = true
    )

    val pagingSource = LegacyPagingSource(
      dataSource = dataSourceFactory.create(),
      fetchDispatcher = pagingFetchDispatcher
    )

    val pager = Pager(
      config = pagingConfig,
      initialKey = null,
      pagingSourceFactory = { pagingSource }
    )

    return CatalogFeedWithoutGroups(
      arguments = arguments,
      bookItems = pager.flow
        .cachedIn(viewModelScope) // Cache before mapping so we always build fresh BookItems
        .map { buildBookItems(it) },
      facetsInOrder = feed.facetsOrder,
      facetsByGroup = feed.facetsByGroup,
      search = feed.feedSearch,
      title = feed.feedTitle
    )
  }

  @VisibleForTesting
  internal fun buildBookItems(entries: PagingData<FeedEntry>): PagingData<BookItem> {
    return entries.map {
      when (it) {
        is FeedEntry.FeedEntryCorrupt -> BookItem.Corrupt(it)
        is FeedEntry.FeedEntryOPDS -> {
          val bookWithStatus = bookRegistry.bookOrNull(it.bookID) ?: synthesizeBookWithStatus(it)
          buildBookItem(it, bookWithStatus, this)
        }
      }
    }
  }

  private class CatalogFacetPseudoTitleProvider(
    val resources: Resources
  ) : FeedFacetPseudoTitleProviderType {
    override val sortByTitle: String
      get() = resources.getString(R.string.feedByTitle)
    override val sortByAuthor: String
      get() = resources.getString(R.string.feedByAuthor)
    override val collection: String
      get() = resources.getString(R.string.feedCollection)
    override val collectionAll: String
      get() = resources.getString(R.string.feedCollectionAll)
    override val sortBy: String
      get() = resources.getString(R.string.feedSortBy)
  }

  val accountProvider: AccountProviderType?
    get() =
      try {
        when (val ownership = feedArguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount ->
            profilesController.profileCurrent()
              .account(ownership.accountId)
              .provider
          is CatalogFeedOwnership.CollectedFromAccounts ->
            null
        }
      } catch (e: Exception) {
        null
      }

  fun isAccountCatalogRoot(): Boolean {
    val parameters = feedArguments
    if (parameters !is CatalogFeedArgumentsRemote) {
      return false
    }

    val ownership = feedArguments.ownership
    if (ownership !is CatalogFeedOwnership.OwnedByAccount) {
      return false
    }

    val account =
      profilesController.profileCurrent()
        .account(ownership.accountId)

    return account.feedIsRoot(parameters.feedURI)
  }

  /**
   * Set synthesized birthdate based on if user is over 13
   */

  fun updateBirthYear(over13: Boolean) {
    profilesController.profileUpdate { description ->
      val years = if (over13) 14 else 0
      synthesizeDateOfBirthDescription(description, years)
    }.map(::onAgeUpdateCompleted, uiExecutor)
  }

  private fun synthesizeDateOfBirthDescription(
    description: ProfileDescription,
    years: Int
  ): ProfileDescription {
    val newPreferences =
      description.preferences.copy(dateOfBirth = synthesizeDateOfBirth(years))
    return description.copy(preferences = newPreferences)
  }

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth {
    return ProfileDateOfBirth(
      date = DateTime.now().minusYears(years),
      isSynthesized = true
    )
  }

  fun showFeedErrorDetails(failure: FeedLoaderResult.FeedLoaderFailure) {
    listener.post(
      CatalogFeedEvent.OpenErrorPage(
        errorPageParameters(failure)
      )
    )
  }

  private fun errorPageParameters(
    failure: FeedLoaderResult.FeedLoaderFailure
  ): ErrorPageParameters {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep(resources.getString(R.string.catalogFeedLoading))
    taskRecorder.addAttributes(failure.attributes)
    taskRecorder.currentStepFailed(failure.message, "feedLoadingFailed", failure.exception)
    val taskFailure = taskRecorder.finishFailure<Unit>()

    return ErrorPageParameters(
      emailAddress = buildConfiguration.supportErrorReportEmailAddress,
      body = "",
      subject = buildConfiguration.supportErrorReportSubject,
      attributes = taskFailure.attributes.toSortedMap(),
      taskSteps = taskFailure.steps
    )
  }

  fun performSearch(search: FeedSearch, query: String) {
    logSearchToAnalytics(query)
    val feedArguments = resolveSearch(search, query)
    listener.post(
      CatalogFeedEvent.OpenFeed(feedArguments)
    )
  }

  private fun logSearchToAnalytics(query: String) {
    try {
      val profile = profilesController.profileCurrent()
      val accountId =
        when (val ownership = feedArguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> ownership.accountId
          is CatalogFeedOwnership.CollectedFromAccounts -> null
        }

      if (accountId != null) {
        val account = profile.account(accountId)
        analytics.publishEvent(
          AnalyticsEvent.CatalogSearched(
            timestamp = LocalDateTime.now(),
            account = account,
            profileUUID = profile.id.uuid,
            accountProvider = account.provider.id,
            accountUUID = account.id.uuid,
            searchQuery = query
          )
        )
      }
    } catch (e: Exception) {
      logger.error("could not log to analytics: ", e)
    }
  }

  /**
   * Execute the given search based on the current feed.
   */

  private fun resolveSearch(
    search: FeedSearch,
    query: String
  ): CatalogFeedArguments {
    return when (val currentArguments = feedArguments) {
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

  fun openFeed(title: String, uri: URI) {
    val feedArguments = resolveFeed(title, uri, false)
    listener.post(
      CatalogFeedEvent.OpenFeed(feedArguments)
    )
  }

  /**
   * Resolve a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the current feed arguments in order to produce new arguments to load another feed.
   *
   * @param title The title of the target feed
   * @param uri The URI of the target feed
   * @param isSearchResults `true` if the target feed refers to search results
   */

  private fun resolveFeed(
    title: String,
    uri: URI,
    isSearchResults: Boolean
  ): CatalogFeedArguments {
    return when (val arguments = feedArguments) {
      is CatalogFeedArgumentsRemote ->
        CatalogFeedArgumentsRemote(
          feedURI = arguments.feedURI.resolve(uri).normalize(),
          isSearchResults = isSearchResults,
          ownership = arguments.ownership,
          title = title
        )

      is CatalogFeedArgumentsLocalBooks -> {
        throw IllegalStateException(
          "Can't transition local to remote feed: ${feedArguments.title} -> $title"
        )
      }
    }
  }

  fun openFacet(facet: FeedFacet) {
    val feedArguments = resolveFacet(facet)
    listener.post(
      CatalogFeedEvent.OpenFeed(feedArguments)
    )
  }

  /**
   * Resolve a given facet as a set of feed arguments.
   *
   * @param facet The facet
   */

  private fun resolveFacet(
    facet: FeedFacet
  ): CatalogFeedArguments {
    return when (val currentArguments = feedArguments) {
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

  override fun openBookDetail(opdsEntry: FeedEntry.FeedEntryOPDS) {
    listener.post(
      CatalogFeedEvent.OpenBookDetail(feedArguments, opdsEntry)
    )
  }

  override fun openViewer(book: Book, format: BookFormat) {
    listener.post(CatalogFeedEvent.OpenViewer(book, format))
  }

  override fun showTaskError(book: Book, result: TaskResult.Failure<*>) {
    logger.debug("showing error: {}", book.id)

    val errorPageParameters = ErrorPageParameters(
      emailAddress = buildConfiguration.supportErrorReportEmailAddress,
      body = "",
      subject = buildConfiguration.supportErrorReportSubject,
      attributes = result.attributes.toSortedMap(),
      taskSteps = result.steps
    )
    listener.post(CatalogFeedEvent.OpenErrorPage(errorPageParameters))
  }

  override fun dismissBorrowError(feedEntry: FeedEntry.FeedEntryOPDS) {
    borrowViewModel.tryDismissBorrowError(feedEntry.accountID, feedEntry.bookID)
  }

  override fun dismissRevokeError(feedEntry: FeedEntry.FeedEntryOPDS) {
    borrowViewModel.tryDismissRevokeError(feedEntry.accountID, feedEntry.bookID)
  }

  override fun delete(feedEntry: FeedEntry.FeedEntryOPDS) {
    borrowViewModel.tryDelete(feedEntry.accountID, feedEntry.bookID)
  }

  override fun borrowMaybeAuthenticated(book: Book) {
    openLoginDialogIfNecessary(book.account)
    borrowViewModel.tryBorrowMaybeAuthenticated(book)
  }

  override fun reserveMaybeAuthenticated(book: Book) {
    openLoginDialogIfNecessary(book.account)
    borrowViewModel.tryReserveMaybeAuthenticated(book)
  }

  override fun revokeMaybeAuthenticated(book: Book) {
    openLoginDialogIfNecessary(book.account)
    borrowViewModel.tryRevokeMaybeAuthenticated(book)
  }

  private fun openLoginDialogIfNecessary(accountID: AccountID) {
    if (borrowViewModel.isLoginRequired(accountID)) {
      listener.post(
        CatalogFeedEvent.LoginRequired(accountID)
      )
    }
  }

  @VisibleForTesting
  internal fun buildBookItem(
    entry: FeedEntry.FeedEntryOPDS,
    bookWithStatus: BookWithStatus,
    listener: CatalogPagedViewListener
  ): BookItem {
    return when (val status = bookWithStatus.status) {
      /*
      * Error States
      */
      is BookStatus.FailedDownload -> {
        downloadingBooks.remove(bookWithStatus.book.id)
        BookItem.Error(
          entry = entry,
          failure = status.result,
          actions = object : BookItem.Error.ErrorActions {
            override fun dismiss() = listener.dismissBorrowError(entry)
            override fun details() = listener.showTaskError(bookWithStatus.book, status.result)
            override fun retry() = listener.borrowMaybeAuthenticated(bookWithStatus.book)
          }
        )
      }
      is BookStatus.FailedLoan -> {
        downloadingBooks.remove(bookWithStatus.book.id)
        BookItem.Error(
          entry = entry,
          failure = status.result,
          actions = object : BookItem.Error.ErrorActions {
            override fun dismiss() = listener.dismissBorrowError(entry)
            override fun details() = listener.showTaskError(bookWithStatus.book, status.result)
            override fun retry() = listener.borrowMaybeAuthenticated(bookWithStatus.book)
          }
        )
      }
      is BookStatus.FailedRevoke -> {
        BookItem.Error(
          entry = entry,
          failure = status.result,
          actions = object : BookItem.Error.ErrorActions {
            override fun dismiss() = listener.dismissBorrowError(entry)
            override fun details() = listener.showTaskError(bookWithStatus.book, status.result)
            override fun retry() = listener.revokeMaybeAuthenticated(bookWithStatus.book)
          }
        )
      }

      /*
      * Idle States
      */
      is BookStatus.Held.HeldInQueue -> {
        val primaryButton = if (status.isRevocable) {
          BookItem.Idle.IdleButtonConfig(
            getText = { context -> context.getString(R.string.catalogCancelHold) },
            getDescription = { context -> context.getString(R.string.catalogAccessibilityBookRevokeHold) },
            onClick = { listener.revokeMaybeAuthenticated(bookWithStatus.book) }
          )
        } else null

        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = primaryButton
            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          }
        )
      }
      is BookStatus.Held.HeldReady -> {
        val primaryButton = BookItem.Idle.IdleButtonConfig(
          getText = { context -> context.getString(R.string.catalogCancelHold) },
          getDescription = { context -> context.getString(R.string.catalogAccessibilityBookRevokeHold) },
          onClick = { listener.revokeMaybeAuthenticated(bookWithStatus.book) }
        )

        val secondaryButton = BookItem.Idle.IdleButtonConfig(
          getText = { context -> context.getString(R.string.catalogGet) },
          getDescription = { context -> context.getString(R.string.catalogAccessibilityBookBorrow) },
          onClick = { listener.borrowMaybeAuthenticated(bookWithStatus.book) }
        )

        val buttons = if (status.isRevocable) {
          primaryButton to secondaryButton
        } else secondaryButton to null

        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = buttons.first
            override fun secondaryButton() = buttons.second
          }
        )
      }
      is BookStatus.Holdable -> {
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = BookItem.Idle.IdleButtonConfig(
              getText = { context -> context.getString(R.string.catalogReserve) },
              getDescription = { context -> context.getString(R.string.catalogAccessibilityBookReserve) },
              onClick = { listener.reserveMaybeAuthenticated(bookWithStatus.book) }
            )

            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          }
        )
      }
      is BookStatus.Loanable -> {
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = BookItem.Idle.IdleButtonConfig(
              getText = { context -> context.getString(R.string.catalogGet) },
              getDescription = { context -> context.getString(R.string.catalogAccessibilityBookBorrow) },
              onClick = { listener.borrowMaybeAuthenticated(bookWithStatus.book) }
            )

            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          }
        )
      }
      is BookStatus.Revoked -> {
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton(): BookItem.Idle.IdleButtonConfig? = null
            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          }
        )
      }
      is BookStatus.Loaned.LoanedDownloaded -> {
        val justDownloaded = downloadingBooks.remove(bookWithStatus.book.id)?.let { true } ?: false

        val textConfig = when (val format = bookWithStatus.book.findPreferredFormat()) {
          is BookFormat.BookFormatEPUB,
          is BookFormat.BookFormatPDF -> {
            Triple(
              { context: Context -> context.getString(R.string.catalogRead) },
              { context: Context -> context.getString(R.string.catalogAccessibilityBookRead) },
              format
            )
          }
          is BookFormat.BookFormatAudioBook ->
            Triple(
              { context: Context -> context.getString(R.string.catalogListen) },
              { context: Context -> context.getString(R.string.catalogAccessibilityBookListen) },
              format
            )
          else -> null
        }

        val primaryButton = textConfig?.let { (text, description, format) ->
          BookItem.Idle.IdleButtonConfig(
            getText = text,
            getDescription = description,
            onClick = { listener.openViewer(bookWithStatus.book, format) }
          )
        }

        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = primaryButton
            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          },
          loanExpiry = status.loanExpiryDate,
          downloadState = if (justDownloaded) DownloadState.Complete else null
        )
      }
      is BookStatus.Loaned.LoanedNotDownloaded -> {
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() = listener.openBookDetail(entry)
            override fun primaryButton() = BookItem.Idle.IdleButtonConfig(
              getText = { context -> context.getString(R.string.catalogDownload) },
              getDescription = { context -> context.getString(R.string.catalogAccessibilityBookDownload) },
              onClick = { listener.borrowMaybeAuthenticated(bookWithStatus.book) }
            )

            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          },
          loanExpiry = status.loanExpiryDate
        )
      }

      /*
      * Progress States
      */
      is BookStatus.RequestingDownload,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingRevoke,
      is BookStatus.DownloadExternalAuthenticationInProgress,
      is BookStatus.DownloadWaitingForExternalAuthentication -> {
        val previousDownloadStatus = downloadingBooks.replace(bookWithStatus.book.id, status)
        val newlyDownloading = previousDownloadStatus == null
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() {}
            override fun primaryButton(): BookItem.Idle.IdleButtonConfig? = null
            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          },
          downloadState = DownloadState.InProgress(isStarting = newlyDownloading)
        )
      }
      is BookStatus.Downloading -> {
        val previousDownloadStatus = downloadingBooks.replace(bookWithStatus.book.id, status)
        val newlyDownloading = previousDownloadStatus == null
        BookItem.Idle(
          entry = entry,
          actions = object : BookItem.Idle.IdleActions {
            override fun openBookDetail() {}
            override fun primaryButton(): BookItem.Idle.IdleButtonConfig? = null
            override fun secondaryButton(): BookItem.Idle.IdleButtonConfig? = null
          },
          downloadState = DownloadState.InProgress(
            progress = status.progressPercent?.toInt(),
            isStarting = newlyDownloading
          )
        )
      }
    }
  }
}
