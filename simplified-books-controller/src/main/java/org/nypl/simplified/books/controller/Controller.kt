package org.nypl.simplified.books.controller

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.NullCheck
import org.joda.time.Instant
import org.joda.time.LocalDate
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.books.book_registry.BookStatusDownloadResult
import org.nypl.simplified.books.book_registry.BookStatusRevokeResult
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
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
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.AccountCreateTaskResult
import org.nypl.simplified.profiles.controller.api.AccountDeleteTaskResult
import org.nypl.simplified.profiles.controller.api.AccountLoginTaskResult
import org.nypl.simplified.profiles.controller.api.AccountLogoutTaskResult
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
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
  private val accountProviders: AccountProviderRegistryType,
  private val adobeDrm: AdobeAdeptExecutorType?,
  private val analytics: AnalyticsType,
  private val bookRegistry: BookRegistryType,
  private val borrowStrings: BookBorrowStringResourcesType,
  private val bundledContent: BundledContentResolverType,
  private val cacheDirectory: File,
  private val downloader: DownloaderType,
  private val feedLoader: FeedLoaderType,
  private val feedParser: OPDSFeedParserType,
  private val http: HTTPType,
  private val patronUserProfileParsers: PatronUserProfileParsersType,
  private val profileAccountCreationStringResources: ProfileAccountCreationStringResourcesType,
  private val profileAccountDeletionStringResources: ProfileAccountDeletionStringResourcesType,
  private val profileEvents: ObservableType<ProfileEvent>,
  private val profiles: ProfilesDatabaseType,
  private val readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>,
  private val revokeStrings: BookRevokeStringResourcesType,
  private val taskExecutor: ListeningExecutorService,
  private val timerExecutor: ExecutorService
) : BooksControllerType, ProfilesControllerType {

  private val accountRegistrySubscription: ObservableSubscriptionType<AccountProviderRegistryEvent>
  private val profileEventSubscription: ObservableSubscriptionType<ProfileEvent>
  private val timer = ProfileIdleTimer.create(this.timerExecutor, this.profileEvents)
  private val downloads: ConcurrentHashMap<BookID, DownloadType> =
    ConcurrentHashMap(32)

  private val logger =
    LoggerFactory.getLogger(Controller::class.java)

  init {
    this.profileEventSubscription =
      this.profileEvents.subscribe { this.onProfileEvent(it) }
    this.accountRegistrySubscription =
      this.accountProviders.events.subscribe { event -> this.onAccountRegistryEvent(event) }

    /*
     * If the anonymous profile is enabled, then ensure that it is "selected" and will
     * therefore very shortly have all of its books loaded.
     */

    if (this.profiles.anonymousProfileEnabled() == ANONYMOUS_PROFILE_ENABLED) {
      this.logger.debug("initializing anonymous profile")
      this.profileSelect(this.profileCurrent().id)
    }
  }

  /**
   * Respond to account registry events.
   */

  private fun onAccountRegistryEvent(event: AccountProviderRegistryEvent) {
    if (!this.profileAnyIsCurrent()) {
      return
    }

    return when (event) {
      is AccountProviderRegistryEvent.Updated ->
        this.onAccountRegistryProviderUpdatedEvent(event)
      is AccountProviderRegistryEvent.SourceFailed -> {

      }
    }
  }

  private fun onAccountRegistryProviderUpdatedEvent(event: AccountProviderRegistryEvent.Updated) {
    val profileCurrentOpt = this.profiles.currentProfile()
    if (profileCurrentOpt is Some<ProfileType>) {
      val profileCurrent = profileCurrentOpt.get()
      this.taskExecutor.submit(ProfileAccountProviderUpdatedTask(
        profile = profileCurrent,
        accountProviderID = event.id,
        accountProviders = this.accountProviders))
      Unit
    } else {

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

  override fun profileAccountCreate(provider: URI): FluentFuture<AccountCreateTaskResult> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountCreateTask(
        this.accountEvents,
        provider,
        this.accountProviders,
        this.profiles,
        this.profileAccountCreationStringResources)))
  }

  override fun profileAccountDeleteByProvider(provider: URI): FluentFuture<AccountDeleteTaskResult> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountDeleteTask(
        this.accountEvents,
        provider,
        this.profiles,
        this.profileEvents,
        this.profileAccountDeletionStringResources)))
  }

  override fun profileAccountSelectByProvider(provider: URI): FluentFuture<ProfileAccountSelectEvent> {
    return FluentFuture.from(this.taskExecutor.submit(
      ProfileAccountSelectionTask(
        this.profiles,
        this.profileEvents,
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
    return ImmutableList.sortedCopyOf(
      this.profileCurrent()
        .accountsByProvider()
        .values
        .map { account -> account.provider })
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
    entry: OPDSAcquisitionFeedEntry): FluentFuture<BookStatusDownloadResult> {

    return FluentFuture.from(this.taskExecutor.submit(BookBorrowTask(
      account = account,
      acquisition = acquisition,
      adobeDRM = this.adobeDrm,
      bookId = id,
      bookRegistry = this.bookRegistry,
      borrowStrings = this.borrowStrings,
      bundledContent = this.bundledContent,
      cacheDirectory = this.cacheDirectory,
      clock = { Instant.now() },
      downloader = this.downloader,
      downloads = this.downloads,
      feedLoader = this.feedLoader,
      entry = entry)))
  }

  override fun bookBorrowFailedDismiss(
    account: AccountType,
    id: BookID) {

    NullCheck.notNull(account, "Account")
    NullCheck.notNull(id, "Book ID")

    this.taskExecutor.submit(BookBorrowFailedDismissTask(
      this.downloader,
      this.downloads,
      account.bookDatabase,
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
    bookId: BookID): FluentFuture<BookStatusRevokeResult> {
    return FluentFuture.from(this.taskExecutor.submit(BookRevokeTask(
      account,
      this.adobeDrm,
      bookId,
      this.bookRegistry,
      this.feedLoader,
      this.revokeStrings)))
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
      account.bookDatabase,
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
      accountProviders: AccountProviderRegistryType,
      adobeDrm: AdobeAdeptExecutorType?,
      analytics: AnalyticsType,
      bookBorrowStrings: BookBorrowStringResourcesType,
      bookRegistry: BookRegistryType,
      bundledContent: BundledContentResolverType,
      cacheDirectory: File,
      downloader: DownloaderType,
      exec: ExecutorService,
      feedLoader: FeedLoaderType,
      feedParser: OPDSFeedParserType,
      http: HTTPType,
      patronUserProfileParsers: PatronUserProfileParsersType,
      profileAccountCreationStringResources: ProfileAccountCreationStringResourcesType,
      profileAccountDeletionStringResources: ProfileAccountDeletionStringResourcesType,
      profileEvents: ObservableType<ProfileEvent>,
      profiles: ProfilesDatabaseType,
      readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>,
      revokeStrings: BookRevokeStringResourcesType,
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
        borrowStrings = bookBorrowStrings,
        bundledContent = bundledContent,
        cacheDirectory = cacheDirectory,
        downloader = downloader,
        feedLoader = feedLoader,
        feedParser = feedParser,
        http = http,
        patronUserProfileParsers = patronUserProfileParsers,
        profileAccountCreationStringResources = profileAccountCreationStringResources,
        profileAccountDeletionStringResources = profileAccountDeletionStringResources,
        profileEvents = profileEvents,
        profiles = profiles,
        readerBookmarkEvents = readerBookmarkEvents,
        revokeStrings = revokeStrings,
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
