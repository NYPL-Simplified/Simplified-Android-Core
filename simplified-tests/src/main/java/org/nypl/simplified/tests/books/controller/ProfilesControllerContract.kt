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
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountBarcode
import org.nypl.simplified.books.accounts.AccountBundledCredentialsEmpty
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventCreation.AccountCreationSucceeded
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginSucceeded
import org.nypl.simplified.books.accounts.AccountEventUpdated
import org.nypl.simplified.books.accounts.AccountPIN
import org.nypl.simplified.books.accounts.AccountProvider
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription
import org.nypl.simplified.books.accounts.AccountProviderCollection
import org.nypl.simplified.books.accounts.AccountsDatabases
import org.nypl.simplified.books.analytics.AnalyticsLogger
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.ProfileFeedRequest
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.feeds.FeedHTTPTransport
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationFailed
import org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED
import org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationSucceeded
import org.nypl.simplified.books.profiles.ProfileDatabaseException
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException
import org.nypl.simplified.books.profiles.ProfilePreferencesChanged
import org.nypl.simplified.books.profiles.ProfileSelected
import org.nypl.simplified.books.profiles.ProfilesDatabase
import org.nypl.simplified.books.profiles.ProfilesDatabaseType
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderColorScheme
import org.nypl.simplified.books.reader.ReaderFontSelection
import org.nypl.simplified.books.reader.ReaderPreferences
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.http.MockingHTTP
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

  protected abstract fun context(): Context

  private fun fakeProvider(provider_id: String): AccountProvider {
    return AccountProvider.builder()
      .setId(URI.create(provider_id))
      .setDisplayName("Fake Library")
      .setSubtitle(Option.some("Imaginary books"))
      .setLogo(Option.some(URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo=")))
      .setCatalogURI(URI.create("http://example.com/accounts0/feed.xml"))
      .setSupportEmail("postmaster@example.com")
      .setAnnotationsURI(Option.some(URI.create("http://example.com/accounts0/annotations")))
      .setPatronSettingsURI(Option.some(URI.create("http://example.com/accounts0/patrons/me")))
      .build()
  }

  private fun controller(
    taskExecutor: ExecutorService,
    feedsExecutor: ListeningExecutorService,
    http: HTTPType,
    books: BookRegistryType,
    profiles: ProfilesDatabaseType,
    downloader: DownloaderType,
    accountProviders: FunctionType<Unit, AccountProviderCollection>,
    timerExecutor: ExecutorService): ProfilesControllerType {

    val parser =
      OPDSFeedParser.newParser(
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes()))
    val transport =
      FeedHTTPTransport.newTransport(http)
    val bundledContent = BundledContentResolverType {
      uri -> throw FileNotFoundException(uri.toString())
    }

    val feedLoader =
      FeedLoader.create(
        feedsExecutor,
        parser,
        OPDSSearchParser.newParser(),
        transport,
        books,
        bundledContent)

    val analyticsDirectory =
      File("/tmp/simplified-android-tests")
    val analyticsLogger =
      AnalyticsLogger.create(analyticsDirectory)

    return Controller.create(
      exec = taskExecutor,
      accountEvents = this.accountEvents,
      profileEvents = this.profileEvents,
      readerBookmarkEvents = this.readerBookmarkEvents,
      http = http,
      feedParser = parser,
      feedLoader = feedLoader,
      downloader = downloader,
      profiles = profiles,
      analyticsLogger = analyticsLogger,
      bookRegistry = books,
      bundledContent = bundledContent,
      accountProviders = accountProviders,
      timerExecutor = timerExecutor,
      adobeDrm = null)
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
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
        this.executorBooks,
        this.executorFeeds,
        this.http,
        this.bookRegistry,
        profiles,
        downloader,
        FunctionType { this.accountProviders(it) },
        this.executorTimer)

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

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val downloader = DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    val controller = this.controller(
      this.executorBooks,
      this.executorFeeds,
      this.http,
      this.bookRegistry,
      profiles,
      downloader,
      FunctionType { this.accountProviders(it) },
      this.executorTimer)

    val account_provider = this.accountProviders().providerDefault()
    controller.profileCreate(account_provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()

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

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })

    val date = LocalDate.now()
    val provider = this.accountProviders().providerDefault()
    controller.profileCreate(provider, "Kermit", "Female", date).get()
    controller.profileCreate(provider, "Kermit", "Female", date).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isTypeAndMatches(ProfileCreationFailed::class.java, this.profileEventsReceived!!, 1) { e -> Assert.assertEquals(ERROR_DISPLAY_NAME_ALREADY_USED, e.errorCode()) }
  }

  /**
   * Trying to log in to an account with the wrong credentials should fail.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesAccountLoginFailed() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })
    controller.accountEvents().subscribe({ this.accountEventsReceived.add(it) })

    val provider = this.fakeAuthProvider("urn:fake-auth:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0L,
        HashMap(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none<HTTPProblemReport>()))

    val credentials = AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

    controller.profileAccountLogin(
      profiles.currentProfileUnsafe().accounts().firstKey(), credentials).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isType(ProfileSelected::class.java, this.profileEventsReceived, 1)

    EventAssertions.isType(AccountCreationSucceeded::class.java, this.accountEventsReceived, 0)
    EventAssertions.isType(AccountLoginFailed::class.java, this.accountEventsReceived, 1)

    Assert.assertTrue(
      "Credentials must not be saved",
      controller.profileAccountCurrent().credentials().isNone)
  }

  /**
   * Trying to log in to an account with the right credentials should succeed.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesAccountLoginSucceeded() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })
    controller.accountEvents().subscribe({ this.accountEventsReceived.add(it) })

    val provider = this.fakeAuthProvider("urn:fake-auth:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        HashMap(),
        0L))

    val credentials = AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

    controller.profileAccountLogin(
      profiles.currentProfileUnsafe().accounts().firstKey(), credentials).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isType(ProfileSelected::class.java, this.profileEventsReceived, 1)

    EventAssertions.isType(AccountCreationSucceeded::class.java, this.accountEventsReceived, 0)
    EventAssertions.isType(AccountEventUpdated::class.java, this.accountEventsReceived, 1)
    EventAssertions.isType(AccountLoginSucceeded::class.java, this.accountEventsReceived, 2)

    Assert.assertEquals(
      "Credentials must be saved",
      Option.some(credentials),
      controller.profileAccountCurrent().credentials())
  }

  /**
   * Trying to log in to an account with the wrong credentials should fail.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesAccountCurrentLoginFailed() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })
    controller.accountEvents().subscribe({ this.accountEventsReceived.add(it) })

    val provider = this.fakeAuthProvider("urn:fake-auth:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0L,
        HashMap(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none<HTTPProblemReport>()))

    val credentials = AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

    controller.profileAccountCurrentLogin(credentials).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isType(ProfileSelected::class.java, this.profileEventsReceived, 1)

    EventAssertions.isType(AccountCreationSucceeded::class.java, this.accountEventsReceived, 0)
    EventAssertions.isType(AccountLoginFailed::class.java, this.accountEventsReceived, 1)

    Assert.assertTrue(
      "Credentials must not be saved",
      controller.profileAccountCurrent().credentials().isNone)
  }

  /**
   * Trying to log in to an account with the right credentials should succeed.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesAccountCurrentLoginSucceeded() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })
    controller.accountEvents().subscribe({ this.accountEventsReceived.add(it) })

    val provider = this.fakeAuthProvider("urn:fake-auth:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        HashMap(),
        0L))

    val credentials = AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

    controller.profileAccountCurrentLogin(credentials).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isType(ProfileSelected::class.java, this.profileEventsReceived, 1)

    EventAssertions.isType(AccountCreationSucceeded::class.java, this.accountEventsReceived, 0)
    EventAssertions.isType(AccountEventUpdated::class.java, this.accountEventsReceived, 1)
    EventAssertions.isType(AccountLoginSucceeded::class.java, this.accountEventsReceived, 2)

    Assert.assertEquals(
      "Credentials must be saved",
      Option.some(credentials),
      controller.profileAccountCurrent().credentials())
  }

  /**
   * Trying to log in to an account that doesn't require authentication should trivially succeed.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesAccountCurrentLoginNoAuthSucceeded() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })
    controller.accountEvents().subscribe({ this.accountEventsReceived.add(it) })

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()

    val credentials = AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

    controller.profileAccountCurrentLogin(credentials).get()

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isType(ProfileSelected::class.java, this.profileEventsReceived, 1)

    EventAssertions.isType(AccountCreationSucceeded::class.java, this.accountEventsReceived, 0)
    EventAssertions.isType(AccountLoginSucceeded::class.java, this.accountEventsReceived, 1)

    Assert.assertTrue(
      "Credentials must not be saved",
      controller.profileAccountCurrent().credentials().isNone)
  }

  /**
   * Setting and getting bookmarks works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testProfilesBookmarks() {

    val profiles = this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller = this.controller(
      this.executorBooks,
      this.executorFeeds,
      this.http,
      this.bookRegistry,
      profiles,
      this.downloader,
      FunctionType { this.accountProviders(it) },
      this.executorTimer)

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()
    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })

    controller.profileBookmarkSet(
      BookID.create("aaaa"),
      ReaderBookLocation.create(Option.none(), "1")).get()

    Assert.assertEquals(
      "Bookmark must have been saved",
      Option.some(ReaderBookLocation.create(Option.none(), "1")),
      controller.profileBookmarkGet(BookID.create("aaaa")))

    controller.profileBookmarkSet(
      BookID.create("aaaa"),
      ReaderBookLocation.create(Option.none(), "2")).get()

    Assert.assertEquals(
      "Bookmark must have been saved",
      Option.some(ReaderBookLocation.create(Option.none(), "2")),
      controller.profileBookmarkGet(BookID.create("aaaa")))

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived!!, 0) { e ->
      Assert.assertTrue("Preferences must not have changed", !e.changedReaderPreferences())
      Assert.assertTrue("Bookmarks must have changed", e.changedReaderBookmarks())
    }

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived!!, 1) { e ->
      Assert.assertTrue("Preferences must not have changed", !e.changedReaderPreferences())
      Assert.assertTrue("Bookmarks must have changed", e.changedReaderBookmarks())
    }
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
    val controller = this.controller(
      this.executorBooks,
      this.executorFeeds,
      this.http,
      this.bookRegistry,
      profiles,
      this.downloader,
      FunctionType { this.accountProviders(it) }, this.executorTimer)

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()
    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })

    controller.profilePreferencesUpdate(profiles.currentProfileUnsafe().preferences()).get()

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived!!, 0) { e ->
      Assert.assertTrue("Preferences must not have changed", !e.changedReaderPreferences())
      Assert.assertTrue("Bookmarks must not have changed", !e.changedReaderBookmarks())
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

    EventAssertions.isTypeAndMatches(ProfilePreferencesChanged::class.java, this.profileEventsReceived!!, 0) { e ->
      Assert.assertTrue("Preferences must have changed", e.changedReaderPreferences())
      Assert.assertTrue("Bookmarks must not have changed", !e.changedReaderBookmarks())
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
    val controller = this.controller(this.executorBooks, this.executorFeeds, this.http, this.bookRegistry, profiles, this.downloader, FunctionType { this.accountProviders(it) }, this.executorTimer)

    val provider = this.fakeProvider("urn:fake:0")
    controller.profileCreate(provider, "Kermit", "Female", LocalDate.now()).get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(provider.id()).get()
    controller.profileEvents().subscribe({ this.profileEventsReceived.add(it) })

    val feed = controller.profileFeed(
      ProfileFeedRequest.builder(
        URI.create("Books"), "Books", "Sort by") { t -> "Sort by title" }
        .build())
      .get()

    Assert.assertEquals(0L, feed.size.toLong())
  }

  @Throws(ProfileDatabaseException::class)
  private fun profilesDatabaseWithoutAnonymous(
    dir_profiles: File): ProfilesDatabaseType {
    return ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(Unit.unit()),
      AccountBundledCredentialsEmpty.getInstance(),
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

    val providers = TreeMap<URI, AccountProvider>()
    providers[fake0.id()] = fake0
    providers[fake1.id()] = fake1
    providers[fake2.id()] = fake2
    providers[fake3.id()] = fake3
    return AccountProviderCollection.create(fake0, providers)
  }

  private fun fakeAuthProvider(uri: String): AccountProvider {
    return this.fakeProvider(uri)
      .toBuilder()
      .setAuthentication(Option.some(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create(uri))
        .setPassCodeLength(4)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build()))
      .build()
  }
}
