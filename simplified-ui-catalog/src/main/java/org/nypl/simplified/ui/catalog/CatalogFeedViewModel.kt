package org.nypl.simplified.ui.catalog

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
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
import org.nypl.simplified.books.controller.api.BooksControllerType
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
import org.nypl.simplified.profiles.api.ProfileEvent
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
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
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
  private val booksController: BooksControllerType,
  private val bookRegistry: BookRegistryType,
  private val buildConfiguration: BuildConfigurationServiceType,
  private val analytics: AnalyticsType,
  private val borrowViewModel: CatalogBorrowViewModel,
  private val feedArguments: CatalogFeedArguments,
  private val listener: FragmentListenerType<CatalogFeedEvent>
) : ViewModel(), CatalogPagedViewListener {

  private val instanceId =
    UUID.randomUUID()

  private val logger =
    LoggerFactory.getLogger(this.javaClass)

  private val stateMutable: MutableLiveData<CatalogFeedState> =
    MutableLiveData(CatalogFeedState.CatalogFeedLoading(this.feedArguments))

  init {
    loadFeed(this.feedArguments)
  }

  private val state: CatalogFeedState
    get() = this.stateLive.value!!

  private class BookModel(
    val feedEntry: FeedEntry.FeedEntryOPDS,
    val onBookChanged: MutableList<(BookWithStatus) -> Unit> = mutableListOf()
  )

  private val bookModels: MutableMap<BookID, BookModel> =
    mutableMapOf()

  private data class LoaderResultWithArguments(
    val arguments: CatalogFeedArguments,
    val result: FeedLoaderResult
  )

  @GuardedBy("loaderResults")
  private val loaderResults =
    PublishSubject.create<LoaderResultWithArguments>()

  private val subscriptions =
    CompositeDisposable(
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent),
      this.profilesController.profileEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onProfileEvent),
      this.bookRegistry.bookEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onBookStatusEvent),
      this.loaderResults
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onFeedLoaderResult)
    )

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded -> {
        if (this.state.arguments.isLocallyGenerated) {
          this.reloadFeed()
        }
      }
      is AccountEventLoginStateChanged ->
        this.onLoginStateChanged(event.accountID, event.state)
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
          this.logger.debug("reloading feed due to successful login")
          this.reloadFeed()
        }
      }
      CatalogFeedOwnership.CollectedFromAccounts -> {
        if (
          accountState is AccountLoginState.AccountLoggedIn ||
          accountState is AccountLoginState.AccountNotLoggedIn
        ) {
          this.logger.debug("reloading feed due to successful login or logout")
          this.reloadFeed()
        }
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileUpdated.Succeeded -> {
        when (val ownership = this.state.arguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> {
            val ageChanged =
              event.newDescription.preferences.dateOfBirth != event.oldDescription.preferences.dateOfBirth
            if (ageChanged) {
              val account = this.profilesController.profileCurrent().account(ownership.accountId)
              onAgeUpdateSuccess(account, ownership, event)
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
    this.logger.debug("age updated from {} to {}", oldAge, newAge)

    newAge?.let { age ->
      val newParameters = CatalogFeedArgumentsRemote(
        title = this.state.arguments.title,
        ownership = ownership,
        feedURI = account.catalogURIForAge(age),
        isSearchResults = false
      )
      this.loadFeed(newParameters)
    }
  }

  private fun onBookStatusEvent(event: BookStatusEvent) {
    this.bookModels[event.bookId]?.let { model ->
      model.onBookChanged.forEach { callback ->
        this.notifyBookStatus(model.feedEntry, callback)
      }
    }

    when (event.statusNow) {
      is BookStatus.Held, is BookStatus.Loaned, is BookStatus.Revoked -> {
        if (this.state.arguments.isLocallyGenerated) {
          this.reloadFeed()
        }
      }
    }
  }

  private fun notifyBookStatus(
    feedEntry: FeedEntry.FeedEntryOPDS,
    callback: (BookWithStatus) -> Unit
  ) {
    val bookWithStatus =
      this.bookRegistry.bookOrNull(feedEntry.bookID)
        ?: this.synthesizeBookWithStatus(feedEntry)

    callback(bookWithStatus)
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
    this.logger.debug("Synthesizing {} with status {}", book.id, status)
    return BookWithStatus(book, status)
  }

  override fun onCleared() {
    super.onCleared()
    this.logger.debug("[{}]: deleting viewmodel", this.instanceId)
    this.subscriptions.clear()
  }

  val stateLive: LiveData<CatalogFeedState>
    get() = stateMutable

  fun syncAccounts() {
    when (val arguments = state.arguments) {
      is CatalogFeedArgumentsLocalBooks -> {
        this.syncAccounts(arguments)
      }
      is CatalogFeedArgumentsRemote -> {
      }
    }
  }

  private fun syncAccounts(arguments: CatalogFeedArgumentsLocalBooks) {
    val profile =
      this.profilesController.profileCurrent()
    val accountsToSync =
      if (arguments.filterAccount == null) {
        // Sync all accounts
        this.logger.debug("[{}]: syncing all accounts", this.instanceId)
        profile.accounts()
      } else {
        // Sync the account we're filtering on
        this.logger.debug("[{}]: syncing account {}", this.instanceId, arguments.filterAccount)
        profile.accounts().filterKeys { it == arguments.filterAccount }
      }

    for (account in accountsToSync.keys) {
      this.booksController.booksSync(account)
    }

    // Feed will be automatically reloaded if necessary in response to BookStatus change.
  }

  fun reloadFeed() {
    this.loadFeed(state.arguments)
  }

  private fun loadFeed(
    arguments: CatalogFeedArguments
  ) {
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
  ) {
    this.logger.debug("[{}]: loading local feed {}", this.instanceId, arguments.selection)

    val booksUri = URI.create("Books")

    val request =
      ProfileFeedRequest(
        facetTitleProvider = CatalogFacetPseudoTitleProvider(this.resources),
        feedSelection = arguments.selection,
        filterByAccountID = arguments.filterAccount,
        search = arguments.searchTerms,
        sortBy = arguments.sortBy,
        title = arguments.title,
        uri = booksUri
      )

    val future = this.profilesController.profileFeed(request)
      .map { f -> FeedLoaderResult.FeedLoaderSuccess(f) as FeedLoaderResult }
      .onAnyError { ex -> FeedLoaderResult.wrapException(booksUri, ex) }

    this.createNewStatus(
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
      val newState = CatalogFeedState.CatalogFeedAgeGate(arguments)
      this.stateMutable.value = newState
      return
    }

    val loginState =
      account.loginState
    val authentication =
      AccountAuthenticatedHTTP.createAuthorizationIfPresent(loginState.credentials)

    val future =
      this.feedLoader.fetchURI(
        account = account.id,
        uri = arguments.feedURI,
        auth = authentication,
        method = "GET"
      )

    this.createNewStatus(
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

    this.stateMutable.value = newState

    /*
     * Register a callback that updates the feed status when the future completes.
     */

    future.map { feedLoaderResult ->
      synchronized(loaderResults) {
        val resultWithArguments = LoaderResultWithArguments(arguments, feedLoaderResult)
        this.loaderResults.onNext(resultWithArguments)
      }
    }
  }

  private fun onFeedLoaderResult(resultWithArguments: LoaderResultWithArguments) {
    this.onFeedStatusUpdated(resultWithArguments.result, resultWithArguments.arguments)
  }

  private fun onFeedStatusUpdated(
    result: FeedLoaderResult,
    arguments: CatalogFeedArguments
  ) {
    this.logger.debug("[{}]: feed status updated: {}", this.instanceId, result.javaClass)

    this.stateMutable.value = this.feedLoaderResultToFeedState(arguments, result)
  }

  private fun feedLoaderResultToFeedState(
    arguments: CatalogFeedArguments,
    result: FeedLoaderResult
  ): CatalogFeedState {
    return when (result) {
      is FeedLoaderResult.FeedLoaderSuccess ->
        when (val feed = result.feed) {
          is Feed.FeedWithoutGroups ->
            this.onReceivedFeedWithoutGroups(arguments, feed)
          is Feed.FeedWithGroups ->
            this.onReceivedFeedWithGroups(arguments, feed)
        }
      is FeedLoaderResult.FeedLoaderFailure ->
        this.onReceivedFeedFailure(arguments, result)
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
        when (val ownership = this.state.arguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> {
            val shouldAuthenticate =
              this.profilesController.profileCurrent()
                .account(ownership.accountId)
                .requiresCredentials

            if (shouldAuthenticate) {
              /*
               * Explicitly deferring the opening of the fragment is required due to the
               * tabbed navigation controller eagerly instantiating fragments and causing
               * fragment transaction exceptions. This will go away when we have a replacement
               * for the navigator library.
               */

              this.listener.post(CatalogFeedEvent.LoginRequired(ownership.accountId))
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
      arguments = arguments,
      entries = pagedList,
      facetsInOrder = feed.facetsOrder,
      facetsByGroup = feed.facetsByGroup,
      search = feed.feedSearch,
      title = feed.feedTitle
    )
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

  val accountProvider: AccountProviderType?
    get() =
      try {
        when (val ownership = this.feedArguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount ->
            this.profilesController.profileCurrent()
              .account(ownership.accountId)
              .provider
          is CatalogFeedOwnership.CollectedFromAccounts ->
            null
        }
      } catch (e: Exception) {
        null
      }

  fun isAccountCatalogRoot(): Boolean {
    val parameters = this.feedArguments
    if (parameters !is CatalogFeedArgumentsRemote) {
      return false
    }

    val ownership = this.feedArguments.ownership
    if (ownership !is CatalogFeedOwnership.OwnedByAccount) {
      return false
    }

    val account =
      this.profilesController.profileCurrent()
        .account(ownership.accountId)

    return account.feedIsRoot(parameters.feedURI)
  }

  /**
   * Set synthesized birthdate based on if user is over 13
   */

  fun updateBirthYear(over13: Boolean) {
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

  fun showFeedErrorDetails(failure: FeedLoaderResult.FeedLoaderFailure) {
    this.listener.post(
      CatalogFeedEvent.OpenErrorPage(
        this.errorPageParameters(failure)
      )
    )
  }

  private fun errorPageParameters(
    failure: FeedLoaderResult.FeedLoaderFailure
  ): ErrorPageParameters {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep(this.resources.getString(R.string.catalogFeedLoading))
    taskRecorder.addAttributes(failure.attributes)
    taskRecorder.currentStepFailed(failure.message, "feedLoadingFailed", failure.exception)
    val taskFailure = taskRecorder.finishFailure<Unit>()

    return ErrorPageParameters(
      emailAddress = this.buildConfiguration.supportErrorReportEmailAddress,
      body = "",
      subject = this.buildConfiguration.supportErrorReportSubject,
      attributes = taskFailure.attributes.toSortedMap(),
      taskSteps = taskFailure.steps
    )
  }

  fun performSearch(search: FeedSearch, query: String) {
    this.logSearchToAnalytics(query)
    val feedArguments = this.resolveSearch(search, query)
    this.listener.post(
      CatalogFeedEvent.OpenFeed(feedArguments)
    )
  }

  private fun logSearchToAnalytics(query: String) {
    try {
      val profile = this.profilesController.profileCurrent()
      val accountId =
        when (val ownership = this.feedArguments.ownership) {
          is CatalogFeedOwnership.OwnedByAccount -> ownership.accountId
          is CatalogFeedOwnership.CollectedFromAccounts -> null
        }

      if (accountId != null) {
        val account = profile.account(accountId)
        this.analytics.publishEvent(
          AnalyticsEvent.CatalogSearched(
            timestamp = LocalDateTime.now(),
            credentials = account.loginState.credentials,
            profileUUID = profile.id.uuid,
            accountProvider = account.provider.id,
            accountUUID = account.id.uuid,
            searchQuery = query
          )
        )
      }
    } catch (e: Exception) {
      this.logger.error("could not log to analytics: ", e)
    }
  }

  /**
   * Execute the given search based on the current feed.
   */

  private fun resolveSearch(
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

  fun openFeed(title: String, uri: URI) {
    val feedArguments = this.resolveFeed(title, uri, false)
    this.listener.post(
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

  fun openFacet(facet: FeedFacet) {
    val feedArguments = this.resolveFacet(facet)
    this.listener.post(
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

  override fun openBookDetail(opdsEntry: FeedEntry.FeedEntryOPDS) {
    this.listener.post(
      CatalogFeedEvent.OpenBookDetail(this.feedArguments, opdsEntry)
    )
  }

  override fun openViewer(book: Book, format: BookFormat) {
    this.listener.post(CatalogFeedEvent.OpenViewer(book, format))
  }

  override fun showTaskError(book: Book, result: TaskResult.Failure<*>) {
    this.logger.debug("showing error: {}", book.id)

    val errorPageParameters = ErrorPageParameters(
      emailAddress = this.buildConfiguration.supportErrorReportEmailAddress,
      body = "",
      subject = this.buildConfiguration.supportErrorReportSubject,
      attributes = result.attributes.toSortedMap(),
      taskSteps = result.steps
    )
    this.listener.post(CatalogFeedEvent.OpenErrorPage(errorPageParameters))
  }

  override fun registerObserver(
    feedEntry: FeedEntry.FeedEntryOPDS,
    callback: (BookWithStatus) -> Unit
  ) {
    this.bookModels.getOrPut(feedEntry.bookID, { BookModel(feedEntry) }).onBookChanged.add(callback)
    this.notifyBookStatus(feedEntry, callback)
  }

  override fun unregisterObserver(
    feedEntry: FeedEntry.FeedEntryOPDS,
    callback: (BookWithStatus) -> Unit
  ) {
    val model = this.bookModels[feedEntry.bookID]
    if (model != null) {
      model.onBookChanged.remove(callback)
      if (model.onBookChanged.isEmpty()) {
        this.bookModels.remove(feedEntry.bookID)
      }
    }
  }

  override fun dismissBorrowError(feedEntry: FeedEntry.FeedEntryOPDS) {
    this.borrowViewModel.tryDismissBorrowError(feedEntry.accountID, feedEntry.bookID)
  }

  override fun dismissRevokeError(feedEntry: FeedEntry.FeedEntryOPDS) {
    this.borrowViewModel.tryDismissRevokeError(feedEntry.accountID, feedEntry.bookID)
  }

  override fun delete(feedEntry: FeedEntry.FeedEntryOPDS) {
    this.borrowViewModel.tryDelete(feedEntry.accountID, feedEntry.bookID)
  }

  override fun borrowMaybeAuthenticated(book: Book) {
    this.openLoginDialogIfNecessary(book.account)
    this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
  }

  override fun reserveMaybeAuthenticated(book: Book) {
    this.openLoginDialogIfNecessary(book.account)
    this.borrowViewModel.tryReserveMaybeAuthenticated(book)
  }

  override fun revokeMaybeAuthenticated(book: Book) {
    this.openLoginDialogIfNecessary(book.account)
    this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
  }

  private fun openLoginDialogIfNecessary(accountID: AccountID) {
    if (this.borrowViewModel.isLoginRequired(accountID)) {
      this.listener.post(
        CatalogFeedEvent.LoginRequired(accountID)
      )
    }
  }
}
