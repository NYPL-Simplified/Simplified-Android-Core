package org.nypl.simplified.books.controller

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Some
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject
import org.joda.time.LocalDate
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountDeleteErrorDetails
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusRevokeErrorDetails
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.clock.ClockType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.futures.FluentFutureExtensions
import org.nypl.simplified.futures.FluentFutureExtensions.flatMap
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
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
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
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
  private val cacheDirectory: File,
  private val accountEvents: Subject<AccountEvent>,
  private val profileEvents: Subject<ProfileEvent>,
  private val services: ServiceDirectoryType,
  private val taskExecutor: ListeningExecutorService
) : BooksControllerType, ProfilesControllerType {

  private val accountLoginStringResources =
    this.services.requireService(AccountLoginStringResourcesType::class.java)
  private val accountLogoutStringResources =
    this.services.requireService(AccountLogoutStringResourcesType::class.java)
  private val accountProviderResolutionStrings =
    this.services.requireService(AccountProviderResolutionStringsType::class.java)
  private val accountProviders =
    this.services.requireService(AccountProviderRegistryType::class.java)
  private val adobeDrm =
    this.services.optionalService(AdobeAdeptExecutorType::class.java)
  private val authDocumentParsers =
    this.services.requireService(AuthenticationDocumentParsersType::class.java)
  private val bookRegistry =
    this.services.requireService(BookRegistryType::class.java)
  private val borrowStrings =
    this.services.requireService(BookBorrowStringResourcesType::class.java)
  private val bundledContent =
    this.services.requireService(BundledContentResolverType::class.java)
  private val clock =
    this.services.requireService(ClockType::class.java)
  private val downloader =
    this.services.requireService(DownloaderType::class.java)
  private val feedLoader =
    this.services.requireService(FeedLoaderType::class.java)
  private val feedParser =
    this.services.requireService(OPDSFeedParserType::class.java)
  private val http =
    this.services.requireService(HTTPType::class.java)
  private val patronUserProfileParsers =
    this.services.requireService(PatronUserProfileParsersType::class.java)
  private val profileAccountCreationStringResources =
    this.services.requireService(ProfileAccountCreationStringResourcesType::class.java)
  private val profileAccountDeletionStringResources =
    this.services.requireService(ProfileAccountDeletionStringResourcesType::class.java)
  private val profiles =
    this.services.requireService(ProfilesDatabaseType::class.java)
  private val revokeStrings =
    this.services.requireService(BookRevokeStringResourcesType::class.java)
  private val profileIdleTimer =
    this.services.requireService(ProfileIdleTimerType::class.java)

  private val accountRegistrySubscription: Disposable
  private val downloads: ConcurrentHashMap<BookID, DownloadType> =
    ConcurrentHashMap(32)

  private val logger =
    LoggerFactory.getLogger(Controller::class.java)

  init {
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
      is AccountProviderRegistryEvent.SourceFailed,
      AccountProviderRegistryEvent.StatusChanged -> {

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

  private fun <A> submitTask(task: () -> A): FluentFuture<A> {
    return FluentFuture.from(this.taskExecutor.submit(task))
  }

  private fun <A> submitTask(task: Callable<A>): FluentFuture<A> {
    return FluentFuture.from(this.taskExecutor.submit(task))
  }

  override fun profiles(): SortedMap<ProfileID, ProfileReadableType> {
    return this.castMap(this.profiles.profiles())
  }

  override fun profileAnonymousEnabled(): AnonymousProfileEnabled {
    return this.profiles.anonymousProfileEnabled()
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileCurrent(): ProfileReadableType {
    return this.profiles.currentProfileUnsafe()
  }

  override fun profileEvents(): Observable<ProfileEvent> {
    return this.profileEvents
  }

  override fun profileCreate(
    accountProvider: AccountProviderType,
    displayName: String,
    gender: String,
    date: LocalDate
  ): FluentFuture<ProfileCreationEvent> {
    return this.submitTask(ProfileCreationTask(
      this.profiles,
      this.profileEvents,
      accountProvider,
      displayName,
      gender,
      ProfileDateOfBirth(date = date, isSynthesized = false)))
  }

  override fun profileSelect(
    profileID: ProfileID
  ): FluentFuture<Unit> {
    return this.submitTask(ProfileSelectionTask(
      profiles = this.profiles,
      bookRegistry = this.bookRegistry,
      events = this.profileEvents,
      id = profileID
    ))
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileAccountCurrent(): AccountType {
    val profile = this.profileCurrent()
    return profile.accountCurrent()
  }

  override fun profileAccountLogin(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials
  ): FluentFuture<TaskResult<AccountLoginErrorData, Unit>> {
    return this.submitTask { this.runProfileAccountLogin(accountID, credentials) }
      .flatMap { result -> this.runSyncIfLoginSucceeded(result, accountID) }
  }

  private fun runProfileAccountLogin(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials
  ): TaskResult<AccountLoginErrorData, Unit> {
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
    result: TaskResult<AccountLoginErrorData, Unit>,
    accountID: AccountID
  ): FluentFuture<TaskResult<AccountLoginErrorData, Unit>> {
    return when (result) {
      is TaskResult.Success -> {
        this.logger.debug("logging in succeeded: syncing account")
        val profile = this.profileCurrent()
        val account = profile.account(accountID)
        this.booksSync(account).map { result }
      }
      is TaskResult.Failure -> {
        this.logger.debug("logging in didn't succeed: not syncing account")
        FluentFutureExtensions.fluentFutureOfValue(result)
      }
    }
  }

  override fun profileAccountCreateOrReturnExisting(
    provider: URI
  ): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return this.submitTask(ProfileAccountCreateOrReturnExistingTask(
      accountEvents = this.accountEvents,
      accountProviderID = provider,
      accountProviders = this.accountProviders,
      profiles = this.profiles,
      strings = this.profileAccountCreationStringResources
    ))
  }

  override fun profileAccountCreateCustomOPDS(
    opdsFeed: URI
  ): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return this.submitTask(ProfileAccountCreateCustomOPDSTask(
      accountEvents = this.accountEvents,
      accountProviderRegistry = this.accountProviders,
      authDocumentParsers = this.authDocumentParsers,
      http = this.http,
      opdsURI = opdsFeed,
      opdsFeedParser = this.feedParser,
      profiles = this.profiles,
      resolutionStrings = this.accountProviderResolutionStrings,
      strings = this.profileAccountCreationStringResources
    ))
  }

  override fun profileAccountCreate(
    provider: URI
  ): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return this.submitTask(ProfileAccountCreateTask(
      accountEvents = this.accountEvents,
      accountProviderID = provider,
      accountProviders = this.accountProviders,
      profiles = this.profiles,
      strings = this.profileAccountCreationStringResources
    ))
  }

  override fun profileAccountDeleteByProvider(
    provider: URI
  ): FluentFuture<TaskResult<AccountDeleteErrorDetails, Unit>> {
    return this.submitTask(ProfileAccountDeleteTask(
      accountEvents = this.accountEvents,
      accountProviderID = provider,
      profiles = this.profiles,
      profileEvents = this.profileEvents,
      strings = this.profileAccountDeletionStringResources
    ))
  }

  override fun profileAccountSelectByProvider(
    provider: URI
  ): FluentFuture<ProfileAccountSelectEvent> {
    return this.submitTask(ProfileAccountSelectionTask(
      profiles = this.profiles,
      profileEvents = this.profileEvents,
      accountProvider = provider
    ))
  }

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  override fun profileAccountFindByProvider(provider: URI): AccountType {
    val profile = this.profileCurrent()
    return profile.accountsByProvider()[provider]
      ?: throw AccountsDatabaseNonexistentException("No account with provider: $provider")
  }

  override fun accountEvents(): Observable<AccountEvent> {
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

  override fun profileAccountLogout(
    accountID: AccountID
  ): FluentFuture<TaskResult<AccountLogoutErrorData, Unit>> {
    return this.submitTask {
      val profile = this.profileCurrent()
      val account = profile.account(accountID)
      ProfileAccountLogoutTask(
        adeptExecutor = this.adobeDrm,
        account = account,
        bookRegistry = this.bookRegistry,
        http = this.http,
        logoutStrings = this.accountLogoutStringResources,
        profile = profile
      ).call()
    }
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profilePreferencesUpdate(
    preferences: ProfilePreferences
  ): FluentFuture<Unit> {
    return this.submitTask(ProfilePreferencesUpdateTask(
      events = this.profileEvents,
      profile = this.profiles.currentProfileUnsafe(),
      preferences = preferences
    ))
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun profileFeed(
    request: ProfileFeedRequest
  ): FluentFuture<Feed.FeedWithoutGroups> {
    return this.submitTask(ProfileFeedTask(
      bookRegistry = this.bookRegistry,
      request = request
    ))
  }

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  override fun profileAccountForBook(
    bookID: BookID
  ): AccountType {
    val bookWithStatus = this.bookRegistry.bookOrNull(bookID)
    if (bookWithStatus != null) {
      return this.profileCurrent().account(bookWithStatus.book.account)
    }
    return this.profileAccountCurrent()
  }

  override fun profileIdleTimer(): ProfileIdleTimerType {
    return this.profileIdleTimer
  }

  private fun accountFor(
    accountID: AccountID
  ): FluentFuture<AccountType> {
    return this.submitTask { this.profileCurrent().account(accountID) }
  }

  override fun bookBorrowWithDefaultAcquisition(
    account: AccountType,
    bookID: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return this.submitTask(BookBorrowWithDefaultAcquisitionTask(
      account = account,
      bookId = bookID,
      cacheDirectory = this.cacheDirectory,
      downloads = this.downloads,
      entry = entry,
      services = this.services
    ))
  }

  override fun bookBorrowWithDefaultAcquisition(
    accountID: AccountID,
    bookID: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return this.accountFor(accountID).flatMap { account ->
      this.bookBorrowWithDefaultAcquisition(account, bookID, entry)
    }
  }

  override fun bookBorrow(
    accountID: AccountID,
    bookID: BookID,
    acquisition: OPDSAcquisition,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return this.accountFor(accountID).flatMap { account ->
      this.bookBorrow(account, bookID, acquisition, entry)
    }
  }

  override fun bookBorrow(
    account: AccountType,
    bookID: BookID,
    acquisition: OPDSAcquisition,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return this.submitTask(BookBorrowTask(
      account = account,
      acquisition = acquisition,
      bookId = bookID,
      cacheDirectory = this.cacheDirectory,
      downloads = this.downloads,
      entry = entry,
      services = this.services
    ))
  }

  override fun bookBorrowFailedDismiss(
    account: AccountType,
    bookID: BookID
  ) {
    this.submitTask(BookBorrowFailedDismissTask(
      bookDatabase = account.bookDatabase,
      bookRegistry = this.bookRegistry,
      id = bookID
    ))
  }

  override fun bookBorrowFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  ) {
    this.accountFor(accountID).map { account ->
      this.bookBorrowFailedDismiss(account, bookID)
    }
  }

  private fun downloadCancel(bookID: BookID) {
    this.logger.debug("[{}] download cancel", bookID.brief())
    val existingDownload = this.downloads[bookID]
    if (existingDownload != null) {
      this.logger.debug("[{}] cancelling download {}", existingDownload)
      existingDownload.cancel()
      this.downloads.remove(bookID)
    }
  }

  override fun bookDownloadCancel(
    account: AccountType,
    bookID: BookID
  ) {
    this.downloadCancel(bookID)
  }

  override fun bookDownloadCancel(
    accountID: AccountID,
    bookID: BookID
  ) {
    this.downloadCancel(bookID)
  }

  override fun bookReport(
    account: AccountType,
    feedEntry: FeedEntry.FeedEntryOPDS,
    reportType: String
  ): FluentFuture<Unit> {
    return this.submitTask(BookReportTask(
      http = this.http,
      account = account,
      feedEntry = feedEntry,
      reportType = reportType
    ))
  }

  override fun booksSync(
    account: AccountType
  ): FluentFuture<Unit> {
    return this.submitTask(BookSyncTask(
      booksController = this,
      account = account,
      bookRegistry = this.bookRegistry,
      http = this.http,
      feedParser = this.feedParser
    ))
  }

  override fun bookRevoke(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<TaskResult<BookStatusRevokeErrorDetails, Unit>> {
    return this.submitTask(BookRevokeTask(
      account = account,
      adobeDRM = this.adobeDrm,
      bookID = bookId,
      bookRegistry = this.bookRegistry,
      feedLoader = this.feedLoader,
      revokeStrings = this.revokeStrings
    ))
  }

  override fun bookRevoke(
    accountID: AccountID,
    bookId: BookID
  ): FluentFuture<TaskResult<BookStatusRevokeErrorDetails, Unit>> {
    return this.accountFor(accountID).flatMap { account ->
      this.bookRevoke(account, bookId)
    }
  }

  override fun bookDelete(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<Unit> {
    return this.submitTask(BookDeleteTask(
      account = account,
      bookRegistry = this.bookRegistry,
      bookId = bookId
    ))
  }

  override fun bookRevokeFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  ): FluentFuture<Unit> {
    return this.accountFor(accountID).flatMap { account ->
      this.bookRevokeFailedDismiss(account, bookID)
    }
  }

  override fun bookRevokeFailedDismiss(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<Unit> {
    return this.submitTask(BookRevokeFailedDismissTask(
      bookDatabase = account.bookDatabase,
      bookRegistry = this.bookRegistry,
      bookId = bookId
    ))
  }

  override fun profileAnyIsCurrent(): Boolean =
    this.profiles.currentProfile().isSome

  /**
   * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
   * `V <: VB`.
   */

  private fun <K, VB, V : VB> castMap(m: SortedMap<K, V>): SortedMap<K, VB> {
    return m as SortedMap<K, VB>
  }

  companion object {

    fun createFromServiceDirectory(
      services: ServiceDirectoryType,
      executorService: ExecutorService,
      accountEvents: Subject<AccountEvent>,
      profileEvents: Subject<ProfileEvent>,
      cacheDirectory: File
    ): Controller {
      return Controller(
        services = services,
        cacheDirectory = cacheDirectory,
        accountEvents = accountEvents,
        profileEvents = profileEvents,
        taskExecutor = MoreExecutors.listeningDecorator(executorService)
      )
    }
  }
}
