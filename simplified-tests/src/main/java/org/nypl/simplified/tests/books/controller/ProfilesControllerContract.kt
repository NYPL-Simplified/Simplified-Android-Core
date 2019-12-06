package org.nypl.simplified.tests.books.controller

import android.content.ContentResolver
import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.clock.Clock
import org.nypl.simplified.clock.ClockType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationSucceeded
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.MockAnalytics
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.books.accounts.FakeAccountCredentialStorage
import org.nypl.simplified.tests.books.idle_timer.InoperableIdleTimer
import org.nypl.simplified.tests.http.MockingHTTP
import org.nypl.simplified.tests.strings.MockAccountCreationStringResources
import org.nypl.simplified.tests.strings.MockAccountDeletionStringResources
import org.nypl.simplified.tests.strings.MockAccountLoginStringResources
import org.nypl.simplified.tests.strings.MockAccountLogoutStringResources
import org.nypl.simplified.tests.strings.MockAccountProviderResolutionStrings
import org.nypl.simplified.tests.strings.MockBorrowStringResources
import org.nypl.simplified.tests.strings.MockRevokeStringResources
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class ProfilesControllerContract {

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountEventsReceived: MutableList<AccountEvent>
  private lateinit var authDocumentParsers: AuthenticationDocumentParsersType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var cacheDirectory: File
  private lateinit var contentResolver: ContentResolver
  private lateinit var credentialsStore: FakeAccountCredentialStorage
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var downloader: DownloaderType
  private lateinit var executorBooks: ExecutorService
  private lateinit var executorDownloads: ExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ExecutorService
  private lateinit var http: MockingHTTP
  private lateinit var patronUserProfileParsers: PatronUserProfileParsersType
  private lateinit var profileEvents: PublishSubject<ProfileEvent>
  private lateinit var profileEventsReceived: MutableList<ProfileEvent>
  private lateinit var readerBookmarkEvents: PublishSubject<ReaderBookmarkEvent>

  protected abstract val logger: Logger

  protected abstract fun context(): Context

  private val accountProviderResolutionStrings =
    MockAccountProviderResolutionStrings()
  private val accountLoginStringResources =
    MockAccountLoginStringResources()
  private val accountLogoutStringResources =
    MockAccountLogoutStringResources()
  private val bookBorrowStringResources =
    MockBorrowStringResources()
  private val bookRevokeStringResources =
    MockRevokeStringResources()
  private val profileAccountDeletionStringResources =
    MockAccountDeletionStringResources()
  private val profileAccountCreationStringResources =
    MockAccountCreationStringResources()
  private val analyticsLogger =
    MockAnalytics()

  private fun controller(
    profiles: ProfilesDatabaseType,
    accountProviders: AccountProviderRegistryType
  ): ProfilesControllerType {

    val parser =
      OPDSFeedParser.newParser(
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes()))
    val transport =
      FeedHTTPTransport.newTransport(this.http)
    val bundledContent = BundledContentResolverType { uri ->
      throw FileNotFoundException(uri.toString())
    }

    val feedLoader =
      FeedLoader.create(
        bookRegistry = this.bookRegistry,
        bundledContent = bundledContent,
        exec = this.executorFeeds,
        contentResolver = this.contentResolver,
        parser = parser,
        searchParser = OPDSSearchParser.newParser(),
        transport = transport
      )

    val services = MutableServiceDirectory()
    services.putService(
      AnalyticsType::class.java, analyticsLogger)
    services.putService(
      AccountLoginStringResourcesType::class.java, this.accountLoginStringResources)
    services.putService(
      AccountLogoutStringResourcesType::class.java, this.accountLogoutStringResources)
    services.putService(
      AccountProviderResolutionStringsType::class.java, this.accountProviderResolutionStrings)
    services.putService(
      AccountProviderRegistryType::class.java, accountProviders)
    services.putService(
      AuthenticationDocumentParsersType::class.java, this.authDocumentParsers)
    services.putService(
      BookRegistryType::class.java, this.bookRegistry)
    services.putService(
      BookBorrowStringResourcesType::class.java, this.bookBorrowStringResources)
    services.putService(
      BundledContentResolverType::class.java, bundledContent)
    services.putService(
      DownloaderType::class.java, downloader)
    services.putService(
      FeedLoaderType::class.java, feedLoader)
    services.putService(
      OPDSFeedParserType::class.java, parser)
    services.putService(
      HTTPType::class.java, http)
    services.putService(
      PatronUserProfileParsersType::class.java, patronUserProfileParsers)
    services.putService(
      ProfileAccountCreationStringResourcesType::class.java, profileAccountCreationStringResources)
    services.putService(
      ProfileAccountDeletionStringResourcesType::class.java, profileAccountDeletionStringResources)
    services.putService(
      ProfilesDatabaseType::class.java, profiles)
    services.putService(
      BookRevokeStringResourcesType::class.java, bookRevokeStringResources)
    services.putService(
      ProfileIdleTimerType::class.java, InoperableIdleTimer())
    services.putService(
      ClockType::class.java, Clock)

    return Controller.createFromServiceDirectory(
      services = services,
      contentResolver = this.contentResolver,
      executorService = this.executorBooks,
      accountEvents = accountEvents,
      profileEvents = profileEvents,
      cacheDirectory = this.cacheDirectory
    )
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.credentialsStore = FakeAccountCredentialStorage()
    this.http = MockingHTTP()
    this.authDocumentParsers = Mockito.mock(AuthenticationDocumentParsersType::class.java)
    this.executorDownloads = Executors.newCachedThreadPool()
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorBooks = Executors.newCachedThreadPool()
    this.executorTimer = Executors.newCachedThreadPool()
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.profileEvents = PublishSubject.create<ProfileEvent>()
    this.profileEventsReceived = Collections.synchronizedList(ArrayList())
    this.accountEvents = PublishSubject.create<AccountEvent>()
    this.accountEventsReceived = Collections.synchronizedList(ArrayList())
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.contentResolver = Mockito.mock(ContentResolver::class.java)
    this.readerBookmarkEvents = PublishSubject.create()
    this.bookRegistry = BookRegistry.create()
    this.downloader = DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    this.patronUserProfileParsers = Mockito.mock(PatronUserProfileParsersType::class.java)
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorDownloads.shutdown()
    this.executorTimer.shutdown()
  }

  /**
   * Trying to fetch the current profile without selecting one should fail.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesCurrentNoneCurrent() {

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val downloader =
      DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders())

    this.expected.expect(ProfileNoneCurrentException::class.java)
    controller.profileCurrent()
  }

  /**
   * Selecting a profile works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesCurrentSelectCurrent() {

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val downloader =
      DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders)

    val accountProvider =
      MockAccountProviders.findAccountProviderDangerously(accountProviders, "urn:fake:0")

    controller.profileCreate(accountProvider, "Kermit", "Female", DateTime.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    val p = controller.profileCurrent()
    Assert.assertEquals("Kermit", p.displayName)
  }

  /**
   * Creating a profile with the same display name as an existing profile should fail.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesCreateDuplicate() {

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val accountProviders =
      MockAccountProviders.fakeAccountProviders()
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders
      )

    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val date = DateTime.now()

    val accountProvider =
      MockAccountProviders.findAccountProviderDangerously(accountProviders, "urn:fake:0")

    controller.profileCreate(accountProvider, "Kermit", "Female", date).get()
    controller.profileCreate(accountProvider, "Kermit", "Female", date).get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isTypeAndMatches(ProfileCreationFailed::class.java, this.profileEventsReceived, 1) { e -> Assert.assertEquals(ERROR_DISPLAY_NAME_ALREADY_USED, e.errorCode()) }
  }

  /**
   * Setting and getting preferences works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesPreferences() {

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val accountProviders =
      MockAccountProviders.fakeAccountProviders()
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders)

    val provider =
      MockAccountProviders.findAccountProviderDangerously(accountProviders, "urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", DateTime.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }
    controller.profileUpdate { description -> description }.get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    EventAssertions.isTypeAndMatches(ProfileUpdated.Succeeded::class.java, this.profileEventsReceived, 0) { e ->
      Assert.assertTrue("Preferences must not have changed", e.oldDescription == e.newDescription)
    }

    this.profileEventsReceived.clear()
    controller.profileUpdate { description ->
      description.copy(preferences =
      description.preferences.copy(
        readerPreferences = ReaderPreferences.builder()
          .setBrightness(0.2)
          .setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK)
          .setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC)
          .setFontScale(2.0)
          .build()
      ))
    }.get()

    EventAssertions.isTypeAndMatches(ProfileUpdated.Succeeded::class.java, this.profileEventsReceived, 0) { e ->
      Assert.assertTrue("Preferences must have changed", e.oldDescription != e.newDescription)
    }
  }

  /**
   * Retrieving an empty feed of books works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesFeed() {

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val accountProviders =
      MockAccountProviders.fakeAccountProviders()
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders)

    val provider =
      MockAccountProviders.findAccountProviderDangerously(accountProviders, "urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", DateTime.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val feed =
      controller.profileFeed(
        ProfileFeedRequest(
          uri = URI.create("Books"),
          title = "Books",
          facetGroup = "Sort by",
          facetTitleProvider = FeedFacetPseudoTitleProviderType { t -> "Sort by title" }))
        .get()

    Assert.assertEquals(0L, feed.size.toLong())
  }

  @Throws(ProfileDatabaseException::class)
  private fun profilesDatabaseWithoutAnonymous(dir_profiles: File): ProfilesDatabaseType {
    return ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analyticsLogger,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialsStore,
      AccountsDatabases,
      dir_profiles)
  }

  private fun onAccountResolution(
    id: URI,
    message: String) {
    this.logger.debug("resolution: {}: {}", id, message)
  }
}
