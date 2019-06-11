package org.nypl.simplified.books.controller

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.NullCheck
import org.joda.time.LocalDate
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.futures.FluentFutureExtensions
import org.nypl.simplified.futures.FluentFutureExtensions.flatMap
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelected
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.AccountLoginTaskResult
import org.nypl.simplified.profiles.controller.api.AccountLogoutTaskResult
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ArrayList
import java.util.SortedMap
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * The default controller implementation.
 */

class Controller private constructor(
  private val accountEvents: ObservableType<AccountEvent>,
  private val accountLoginStringResources: AccountLoginStringResourcesType,
  private val accountLogoutStringResources: AccountLogoutStringResourcesType,
  private val accountProviders: FunctionType<Unit, AccountProviderCollectionType>,
  private val adobeDrm: AdobeAdeptExecutorType?,
  private val analytics: AnalyticsType,
  private val bookRegistry: BookRegistryType,
  private val bundledContent: BundledContentResolverType,
  private val downloader: DownloaderType,
  private val feedLoader: FeedLoaderType,
  private val feedParser: OPDSFeedParserType,
  private val http: HTTPType,
  private val patronUserProfileParsers: PatronUserProfileParsersType,
  private val profileEvents: ObservableType<ProfileEvent>,
  private val profiles: ProfilesDatabaseType,
  private val readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>,
  private val taskExecutor: ListeningExecutorService,
  private val timerExecutor: ExecutorService
) : BooksControllerType, ProfilesControllerType {

  private val profileEventSubscription: ObservableSubscriptionType<ProfileEvent>
  private val timer = ProfileIdleTimer.create(this.timerExecutor, this.profileEvents)
  private val downloads: ConcurrentHashMap<BookID, DownloadType> =
    ConcurrentHashMap(32)

  private val logger =
    LoggerFactory.getLogger(Controller::class.java)

  init {
    this.profileEventSubscription = this.profileEvents.subscribe { this.onProfileEvent(it) }

    /*
     * If the anonymous profile is enabled, then ensure that it is "selected" and will
     * therefore very shortly have all of its books loaded.
     */

    if (this.profiles.anonymousProfileEnabled() == ANONYMOUS_PROFILE_ENABLED) {
      this.logger.debug("initializing anonymous profile")
      this.profileSelect(this.profileCurrent().id())
    }
  }

  private fun onProfileEvent(e: ProfileEvent) {
    if (e is ProfileSelected) {
      this.onProfileEventSelected(e)
      return
    }
  }

  private fun onProfileEventSelected(ev: ProfileSelected) {
    this.logger.debug("onProfileEventSelected: {}", ev)

    this.logger.debug("clearing the book registry")
    this.bookRegistry.clear()
    try {
      this.taskExecutor.execute(
        ProfileDataLoadTask(this.profiles.currentProfileUnsafe(), this.bookRegistry))
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }
  }

  override fun profiles(): SortedMap<ProfileID, ProfileReadableType> {
    return castMap(this.profiles.profiles())
  }

  override fun profileAnonymousEnabled(): AnonymousProfileEnabled {
    return this.profiles.anonymousProfileEnabled()
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileCurrent(): ProfileReadableType {
    return this.profiles.currentProfileUnsafe()
  }

  override fun profileEvents(): ObservableReadableType<ProfileEvent> {
    return this.profileEvents
  }

  override fun profileCreate(
    accountProvider: AccountProviderType,
    displayName: String,
    gender: String,
    date: LocalDate): FluentFuture<ProfileCreationEvent> {
    return FluentFuture.from(this.taskExecutor.submit(ProfileCreationTask(
      this.profiles,
      this.profileEvents,
      accountProvider,
      displayName,
      gender,
      ProfileDateOfBirth(date = date, isSynthesized = false))))
  }

  override fun profileSelect(id: ProfileID): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileSelectionTask(this.profiles, this.profileEvents, id)))
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileAccountCurrent(): AccountType {
    val profile = this.profileCurrent()
    return profile.accountCurrent()
  }

  override fun profileAccountLogin(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials): FluentFuture<AccountLoginTaskResult> {

    return FluentFuture.from(
      this.taskExecutor.submit(Callable { this.runProfileAccountLogin(accountID, credentials) }))
      .flatMap { result -> this.runSyncIfLoginSucceeded(result, accountID) }
  }

  private fun runProfileAccountLogin(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials
  ): AccountLoginTaskResult {
    val profile = this.profileCurrent()
    val account = profile.account(accountID)
    return ProfileAccountLoginTask(
      adeptExecutor = this.adobeDrm,
      http = this.http,
      profile = profile,
      account = account,
      loginStrings = this.accountLoginStringResources,
      patronParsers = this.patronUserProfileParsers,
      initialCredentials = credentials
    ).call()
  }

  private fun runSyncIfLoginSucceeded(
    result: AccountLoginTaskResult,
    accountID: AccountID
  ): FluentFuture<AccountLoginTaskResult> {
    return if (result.failed) {
      this.logger.debug("logging in didn't succeed: not syncing account")
      FluentFutureExtensions.fluentFutureOfValue(result)
    } else {
      this.logger.debug("logging in succeeded: syncing account")
      val profile = this.profileCurrent()
      val account = profile.account(accountID)
      this.booksSync(account).map { result }
    }
  }

  override fun profileAccountCreate(provider: URI): FluentFuture<AccountEventCreation> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountCreateTask(
        this.profiles,
        this.accountEvents,
        this.accountProviders,
        provider)))
  }

  override fun profileAccountDeleteByProvider(provider: URI): FluentFuture<AccountEventDeletion> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountDeleteTask(
        this.profiles,
        this.accountEvents,
        this.profileEvents,
        this.accountProviders,
        provider)))
  }

  override fun profileAccountSelectByProvider(provider: URI): FluentFuture<ProfileAccountSelectEvent> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountSelectionTask(
        this.profiles,
        this.profileEvents,
        this.accountProviders,
        provider)))
  }

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  override fun profileAccountFindByProvider(provider: URI): AccountType {
    val profile = this.profileCurrent()
    return profile.accountsByProvider()[provider]
      ?: throw AccountsDatabaseNonexistentException("No account with provider: $provider")
  }

  override fun accountEvents(): ObservableReadableType<AccountEvent> {
    return this.accountEvents
  }

  @Throws(ProfileNoneCurrentException::class, ProfileNonexistentAccountProviderException::class)
  override fun profileCurrentlyUsedAccountProviders(): ImmutableList<AccountProviderType> {
    val accounts = ArrayList<AccountProviderType>()
    val accountProviders = this.accountProviders.call(Unit.unit())
    val profile = this.profileCurrent()

    for (account in profile.accounts().values) {
      val provider = account.provider()
      if (accountProviders.providers().containsKey(provider.id)) {
        val accountProvider = accountProviders.providers()[provider.id]!!
        accounts.add(accountProvider)
      }
    }

    return ImmutableList.sortedCopyOf(accounts)
  }

  override fun profileAccountLogout(account: AccountID): FluentFuture<AccountLogoutTaskResult> {
    return FluentFuture.from(
      this.taskExecutor.submit(Callable {
        val profile = this.profileCurrent()
        val account = profile.account(account)
        ProfileAccountLogoutTask(
          adeptExecutor = this.adobeDrm,
          account = account,
          bookRegistry = this.bookRegistry,
          http = this.http,
          logoutStrings = this.accountLogoutStringResources,
          profile = profile
        ).call()
      }))
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profilePreferencesUpdate(preferences: ProfilePreferences): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfilePreferencesUpdateTask(
        this.profileEvents,
        this.profiles.currentProfileUnsafe(),
        preferences)))
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileFeed(request: ProfileFeedRequest): FluentFuture<Feed.FeedWithoutGroups> {

    NullCheck.notNull(request, "Request")
    return FluentFuture.from(this.taskExecutor.submit(ProfileFeedTask(this.bookRegistry, request)))
  }

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  override fun profileAccountForBook(id: BookID): AccountType {
    NullCheck.notNull(id, "Book ID")

    val bookWithStatus = this.bookRegistry.book(id)

    if (bookWithStatus.isSome) {
      val accountId = (bookWithStatus as Some<BookWithStatus>).get().book().account
      return this.profileCurrent().account(accountId)
    }

    return this.profileAccountCurrent()
  }

  override fun profileIdleTimer(): ProfileIdleTimerType {
    return this.timer
  }

  override fun bookBorrow(
    account: AccountType,
    id: BookID,
    acquisition: OPDSAcquisition,
    entry: OPDSAcquisitionFeedEntry) {

    NullCheck.notNull(account, "Account")
    NullCheck.notNull(id, "Book ID")
    NullCheck.notNull(acquisition, "Acquisition")
    NullCheck.notNull(entry, "Entry")

    this.taskExecutor.submit(BookBorrowTask(
      adobeDRM = this.adobeDrm,
      downloader = this.downloader,
      downloads = this.downloads,
      feedLoader = this.feedLoader,
      bundledContent = this.bundledContent,
      bookRegistry = this.bookRegistry,
      bookId = id,
      account = account,
      acquisition = acquisition,
      entry = entry))
  }

  override fun bookBorrowFailedDismiss(
    account: AccountType,
    id: BookID) {

    NullCheck.notNull(account, "Account")
    NullCheck.notNull(id, "Book ID")

    this.taskExecutor.submit(BookBorrowFailedDismissTask(
      this.downloader,
      this.downloads,
      account.bookDatabase(),
      this.bookRegistry,
      id))
  }

  override fun bookDownloadCancel(
    account: AccountType,
    id: BookID) {

    NullCheck.notNull(account, "Account")
    NullCheck.notNull(id, "Book ID")

    this.logger.debug("[{}] download cancel", id.brief())
    val d = this.downloads[id]
    if (d != null) {
      this.logger.debug("[{}] cancelling download {}", d)
      d.cancel()
      this.downloads.remove(id)
    }
  }

  override fun bookReport(
    account: AccountType,
    feedEntry: FeedEntry.FeedEntryOPDS,
    reportType: String): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(BookReportTask(
      http = this.http,
      account = account,
      feedEntry = feedEntry,
      reportType = reportType)))
  }

  override fun booksSync(account: AccountType): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(BookSyncTask(
      this,
      account,
      this.bookRegistry,
      this.http,
      this.feedParser)))
  }

  override fun bookRevoke(
    account: AccountType,
    bookId: BookID): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(BookRevokeTask(
      this.adobeDrm,
      this.bookRegistry,
      this.feedLoader,
      account,
      bookId)))
  }

  override fun bookDelete(
    account: AccountType,
    bookId: BookID): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(BookDeleteTask(
      account,
      this.bookRegistry,
      bookId)))
  }

  override fun bookRevokeFailedDismiss(
    account: AccountType,
    bookId: BookID): FluentFuture<Unit> {
    return FluentFuture.from(this.taskExecutor.submit(BookRevokeFailedDismissTask(
      account.bookDatabase(),
      this.bookRegistry,
      bookId)))
  }

  override fun profileAnyIsCurrent(): Boolean =
    this.profiles.currentProfile().isSome

  companion object {

    fun create(
      accountEvents: ObservableType<AccountEvent>,
      accountLoginStringResources: AccountLoginStringResourcesType,
      accountLogoutStringResources: AccountLogoutStringResourcesType,
      accountProviders: FunctionType<Unit, AccountProviderCollectionType>,
      adobeDrm: AdobeAdeptExecutorType?,
      analytics: AnalyticsType,
      bookRegistry: BookRegistryType,
      bundledContent: BundledContentResolverType,
      downloader: DownloaderType,
      exec: ExecutorService,
      feedLoader: FeedLoaderType,
      feedParser: OPDSFeedParserType,
      http: HTTPType,
      patronUserProfileParsers: PatronUserProfileParsersType,
      profileEvents: ObservableType<ProfileEvent>,
      profiles: ProfilesDatabaseType,
      readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>,
      timerExecutor: ExecutorService
    ): Controller {
      return Controller(
        accountEvents = accountEvents,
        accountLoginStringResources = accountLoginStringResources,
        accountLogoutStringResources = accountLogoutStringResources,
        accountProviders = accountProviders,
        adobeDrm = adobeDrm,
        analytics = analytics,
        bookRegistry = bookRegistry,
        bundledContent = bundledContent,
        downloader = downloader,
        feedLoader = feedLoader,
        feedParser = feedParser,
        http = http,
        patronUserProfileParsers = patronUserProfileParsers,
        profileEvents = profileEvents,
        profiles = profiles,
        readerBookmarkEvents = readerBookmarkEvents,
        taskExecutor = MoreExecutors.listeningDecorator(exec),
        timerExecutor = timerExecutor
      )
    }

    /**
     * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
     * `V <: VB`.
     */

    private fun <K, VB, V : VB> castMap(m: SortedMap<K, V>): SortedMap<K, VB> {
      return m as SortedMap<K, VB>
    }
  }
}
