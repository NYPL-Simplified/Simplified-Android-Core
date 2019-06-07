package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Unit
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountCreationSucceeded
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountProviders
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountProviderCollection
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.patron.api.PatronSettings
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParserType
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabase
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationSucceeded
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfilePreferencesChanged
import org.nypl.simplified.profiles.api.ProfileSelected
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.MockAccountLoginStringResources
import org.nypl.simplified.tests.MockAnalytics
import org.nypl.simplified.tests.books.accounts.FakeAccountCredentialStorage
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.TreeMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class ProfilesControllerContract {

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  private lateinit var credentialsStore: FakeAccountCredentialStorage
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorDownloads: ExecutorService
  private lateinit var executorBooks: ExecutorService
  private lateinit var executorTimer: ExecutorService
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var http: MockingHTTP
  private lateinit var profileEvents: ObservableType<ProfileEvent>
  private lateinit var profileEventsReceived: MutableList<ProfileEvent>
  private lateinit var accountEventsReceived: MutableList<AccountEvent>
  private lateinit var accountEvents: ObservableType<AccountEvent>
  private lateinit var readerBookmarkEvents: ObservableType<ReaderBookmarkEvent>
  private lateinit var downloader: DownloaderType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var patronUserProfileParsers: PatronUserProfileParsersType

  protected abstract val logger: Logger

  protected abstract fun context(): Context

  private val accountLoginStringResources = MockAccountLoginStringResources()

  private fun fakeProvider(provider_id: String): AccountProviderType {
    return AccountProviders.builder().apply {
      this.id = URI.create(provider_id)
      this.mainColor = "#ff0000"
      this.displayName = "Fake Library"
      this.subtitle = "Imaginary books"
      this.logo = URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo=")
      this.catalogURI = URI.create("http://example.com/accounts0/feed.xml")
      this.supportEmail = "postmaster@example.com"
      this.annotationsURI = URI.create("http://example.com/accounts0/annotations")
      this.patronSettingsURI = URI.create("http://example.com/accounts0/patrons/me")
    }.build()
  }

  private fun controller(
    profiles: ProfilesDatabaseType,
    accountProviders: FunctionType<Unit, AccountProviderCollectionType>
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
        exec = this.executorFeeds,
        parser = parser,
        searchParser = OPDSSearchParser.newParser(),
        transport = transport,
        bookRegistry = this.bookRegistry,
        bundledContent = bundledContent)

    val analyticsLogger =
      MockAnalytics()

    return Controller.create(
      exec = this.executorBooks,
      accountLoginStringResources = this.accountLoginStringResources,
      accountEvents = this.accountEvents,
      profileEvents = this.profileEvents,
      readerBookmarkEvents = this.readerBookmarkEvents,
      http = this.http,
      feedParser = parser,
      feedLoader = feedLoader,
      downloader = this.downloader,
      profiles = profiles,
      analytics = analyticsLogger,
      bookRegistry = this.bookRegistry,
      bundledContent = bundledContent,
      accountProviders = accountProviders,
      timerExecutor = this.executorTimer,
      adobeDrm = null,
      patronUserProfileParsers = this.patronUserProfileParsers
    )
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.credentialsStore = FakeAccountCredentialStorage()
    this.http = MockingHTTP()
    this.executorDownloads = Executors.newCachedThreadPool()
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorBooks = Executors.newCachedThreadPool()
    this.executorTimer = Executors.newCachedThreadPool()
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.profileEvents = Observable.create<ProfileEvent>()
    this.profileEventsReceived = Collections.synchronizedList(ArrayList())
    this.accountEvents = Observable.create<AccountEvent>()
    this.accountEventsReceived = Collections.synchronizedList(ArrayList())
    this.readerBookmarkEvents = Observable.create()
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
        accountProviders = FunctionType { this.accountProviders(it) })

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

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val downloader =
      DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = FunctionType { this.accountProviders(it) })

    val account_provider = this.accountProviders().providerDefault()
    controller.profileCreate(account_provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    val p = controller.profileCurrent()
    Assert.assertEquals("Kermit", p.displayName())
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
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = FunctionType { this.accountProviders(it) }
      )

    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val date = LocalDate.now()
    val provider = this.accountProviders().providerDefault()
    controller.profileCreate(provider, "Kermit", "Female", date).get()
    controller.profileCreate(provider, "Kermit", "Female", date).get()

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

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = FunctionType { this.accountProviders(it) })

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }
    controller.profilePreferencesUpdate(profiles.currentProfileUnsafe().preferences()).get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived, 0) { e ->
      Assert.assertTrue("Preferences must not have changed", !e.changedReaderPreferences())
    }

    this.profileEventsReceived.clear()
    controller.profilePreferencesUpdate(
      profiles.currentProfileUnsafe()
        .preferences()
        .toBuilder()
        .setReaderPreferences(
          ReaderPreferences.builder()
            .setBrightness(0.2)
            .setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK)
            .setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC)
            .setFontScale(2.0)
            .build())
        .build())
      .get()

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived, 0) { e ->
      Assert.assertTrue("Preferences must have changed", e.changedReaderPreferences())
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

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = FunctionType { this.accountProviders(it) }
      )

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val feed = controller.profileFeed(
      ProfileFeedRequest.builder(
        URI.create("Books"), "Books", "Sort by") { t -> "Sort by title" }
        .build())
      .get()

    Assert.assertEquals(0L, feed.size.toLong())
  }

  @Throws(ProfileDatabaseException::class)
  private fun profilesDatabaseWithoutAnonymous(dir_profiles: File): ProfilesDatabaseType {
    return ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(Unit.unit()),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialsStore,
      AccountsDatabases,
      dir_profiles)
  }

  private fun accountProviders(unit: Unit): AccountProviderCollection {
    return this.accountProviders()
  }

  private fun accountProviders(): AccountProviderCollection {
    val fake0 = this.fakeProvider("urn:fake:0")
    val fake1 = this.fakeProvider("urn:fake:1")
    val fake2 = this.fakeProvider("urn:fake:2")
    val fake3 = this.fakeAuthProvider("urn:fake-auth:0")

    val providers = TreeMap<URI, AccountProviderType>()
    providers[fake0.id] = fake0
    providers[fake1.id] = fake1
    providers[fake2.id] = fake2
    providers[fake3.id] = fake3
    return AccountProviderCollection.create(fake0, providers)
  }

  private fun fakeAuthProvider(uri: String): AccountProviderType {
    return this.fakeProvider(uri)
      .toBuilder()
      .apply {
        this.authentication =
          AccountProviderAuthenticationDescription.builder()
            .setLoginURI(URI.create(uri))
            .setPassCodeLength(4)
            .setPassCodeMayContainLetters(true)
            .setRequiresPin(true)
            .build()
      }.build()
  }
}
