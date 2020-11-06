package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.subjects.PublishSubject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.borrowing.BorrowSubtasks
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.MockAccountCreationStringResources
import org.nypl.simplified.tests.MockAccountDeletionStringResources
import org.nypl.simplified.tests.MockAccountLoginStringResources
import org.nypl.simplified.tests.MockAccountLogoutStringResources
import org.nypl.simplified.tests.MockAccountProviderResolutionStrings
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.MockAnalytics
import org.nypl.simplified.tests.MockRevokeStringResources
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.books.accounts.FakeAccountCredentialStorage
import org.nypl.simplified.tests.books.idle_timer.InoperableIdleTimer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.NoSuchElementException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BooksControllerContract {

  private val logger = LoggerFactory.getLogger(BooksControllerContract::class.java)

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountEventsReceived: MutableList<AccountEvent>
  private lateinit var audioBookManifestStrategies: AudioBookManifestStrategiesType
  private lateinit var authDocumentParsers: AuthenticationDocumentParsersType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var borrowSubtasks: BorrowSubtaskDirectoryType
  private lateinit var cacheDirectory: File
  private lateinit var contentResolver: ContentResolverType
  private lateinit var credentialsStore: FakeAccountCredentialStorage
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var executorDownloads: ListeningExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var lsHTTP: LSHTTPClientType
  private lateinit var patronUserProfileParsers: PatronUserProfileParsersType
  private lateinit var profileEvents: PublishSubject<ProfileEvent>
  private lateinit var profileEventsReceived: MutableList<ProfileEvent>
  private lateinit var profiles: ProfilesDatabaseType
  private lateinit var server: MockWebServer

  protected abstract fun context(): Context

  private val accountProviderResolutionStrings =
    MockAccountProviderResolutionStrings()
  private val accountLoginStringResources =
    MockAccountLoginStringResources()
  private val accountLogoutStringResources =
    MockAccountLogoutStringResources()
  private val revokeStringResources =
    MockRevokeStringResources()
  private val profileAccountDeletionStringResources =
    MockAccountDeletionStringResources()
  private val profileAccountCreationStringResources =
    MockAccountCreationStringResources()
  private val analytics =
    MockAnalytics()

  private fun correctCredentials(): AccountAuthenticationCredentials {
    return AccountAuthenticationCredentials.Basic(
      userName = AccountUsername("abcd"),
      password = AccountPassword("1234"),
      adobeCredentials = null,
      authenticationDescription = null
    )
  }

  private fun createController(
    exec: ExecutorService,
    feedExecutor: ListeningExecutorService,
    accountEvents: PublishSubject<AccountEvent>,
    profileEvents: PublishSubject<ProfileEvent>,
    http: LSHTTPClientType,
    books: BookRegistryType,
    profiles: ProfilesDatabaseType,
    accountProviders: AccountProviderRegistryType,
    patronUserProfileParsers: PatronUserProfileParsersType
  ): BooksControllerType {
    val parser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())
    val transport =
      FeedHTTPTransport(http)

    val bundledContent =
      BundledContentResolverType { uri -> throw FileNotFoundException(uri.toString()) }

    val feedLoader =
      FeedLoader.create(
        bookFormatSupport = this.bookFormatSupport,
        bookRegistry = books,
        bundledContent = bundledContent,
        contentResolver = this.contentResolver,
        exec = feedExecutor,
        parser = parser,
        searchParser = OPDSSearchParser.newParser(),
        transport = transport
      )

    val services = MutableServiceDirectory()
    services.putService(AccountLoginStringResourcesType::class.java, this.accountLoginStringResources)
    services.putService(AccountLogoutStringResourcesType::class.java, this.accountLogoutStringResources)
    services.putService(AccountProviderRegistryType::class.java, accountProviders)
    services.putService(AccountProviderResolutionStringsType::class.java, this.accountProviderResolutionStrings)
    services.putService(AnalyticsType::class.java, this.analytics)
    services.putService(AudioBookManifestStrategiesType::class.java, this.audioBookManifestStrategies)
    services.putService(AuthenticationDocumentParsersType::class.java, this.authDocumentParsers)
    services.putService(BookFormatSupportType::class.java, this.bookFormatSupport)
    services.putService(BookRegistryType::class.java, this.bookRegistry)
    services.putService(BorrowSubtaskDirectoryType::class.java, this.borrowSubtasks)
    services.putService(BookRevokeStringResourcesType::class.java, revokeStringResources)
    services.putService(BundledContentResolverType::class.java, bundledContent)
    services.putService(ContentResolverType::class.java, this.contentResolver)
    services.putService(FeedLoaderType::class.java, feedLoader)
    services.putService(LSHTTPClientType::class.java, this.lsHTTP)
    services.putService(OPDSFeedParserType::class.java, parser)
    services.putService(PatronUserProfileParsersType::class.java, patronUserProfileParsers)
    services.putService(ProfileAccountCreationStringResourcesType::class.java, profileAccountCreationStringResources)
    services.putService(ProfileAccountDeletionStringResourcesType::class.java, profileAccountDeletionStringResources)
    services.putService(ProfileIdleTimerType::class.java, InoperableIdleTimer())
    services.putService(ProfilesDatabaseType::class.java, profiles)

    return Controller.createFromServiceDirectory(
      services = services,
      executorService = exec,
      accountEvents = accountEvents,
      profileEvents = profileEvents,
      cacheDirectory = this.cacheDirectory
    )
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.accountEvents = PublishSubject.create<AccountEvent>()
    this.accountEventsReceived = Collections.synchronizedList(ArrayList())
    this.audioBookManifestStrategies = Mockito.mock(AudioBookManifestStrategiesType::class.java)
    this.authDocumentParsers = Mockito.mock(AuthenticationDocumentParsersType::class.java)
    this.bookEvents = Collections.synchronizedList(ArrayList())
    this.bookFormatSupport = Mockito.mock(BookFormatSupportType::class.java)
    this.bookRegistry = BookRegistry.create()
    this.borrowSubtasks = BorrowSubtasks.directory()
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.contentResolver = Mockito.mock(ContentResolverType::class.java)
    this.credentialsStore = FakeAccountCredentialStorage()
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.executorBooks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorDownloads = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorTimer = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.patronUserProfileParsers = Mockito.mock(PatronUserProfileParsersType::class.java)
    this.profileEvents = PublishSubject.create<ProfileEvent>()
    this.profileEventsReceived = Collections.synchronizedList(ArrayList())
    this.profiles = profilesDatabaseWithoutAnonymous(this.accountEvents, this.directoryProfiles)

    this.lsHTTP =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration("simplified-test", "1.0.0")
        )

    this.server = MockWebServer()
    this.server.start()
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorDownloads.shutdown()
    this.executorTimer.shutdown()
    this.server.close()
  }

  /**
   * If the remote side returns a non 401 error code, syncing should fail with an IO exception.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncRemoteNon401() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(400)
        .setBody("")
    )

    this.expected.expect(ExecutionException::class.java)
    this.expected.expectCause(IsInstanceOf.instanceOf<IOException>(IOException::class.java))
    controller.booksSync(account).get()
  }

  /**
   * If the remote side returns a 401 error code, the current credentials should be thrown away.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncRemote401() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(401)
        .setBody("")
    )

    controller.booksSync(account).get()
    Assert.assertEquals(AccountNotLoggedIn, account.loginState)
  }

  /**
   * If the provider does not support authentication, then syncing is impossible and does nothing.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncWithoutAuthSupport() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeProvider(
        providerId = "urn:fake:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    Assert.assertEquals(AccountLoggedIn(correctCredentials()), account.loginState)
    controller.booksSync(account).get()
    Assert.assertEquals(AccountLoggedIn(correctCredentials()), account.loginState)
  }

  /**
   * If the remote side requires authentication but no credentials were provided, nothing happens.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncMissingCredentials() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!

    Assert.assertEquals(AccountNotLoggedIn, account.loginState)
    controller.booksSync(account).get()
    Assert.assertEquals(AccountNotLoggedIn, account.loginState)
  }

  /**
   * If the remote side returns garbage for a feed, an error is raised.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncBadFeed() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("Unlikely!")
    )

    this.expected.expect(ExecutionException::class.java)
    this.expected.expectCause(IsInstanceOf.instanceOf<OPDSParseException>(OPDSParseException::class.java))
    controller.booksSync(account).get()
  }

  /**
   * If the remote side returns books the account doesn't have, new database entries are created.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncNewEntries() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(resource("testBooksSyncNewEntries.xml")))
    )

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })

    Assert.assertEquals(0L, this.bookRegistry.books().size.toLong())
    controller.booksSync(account).get()
    Assert.assertEquals(3L, this.bookRegistry.books().size.toLong())

    this.bookRegistry.bookOrException(
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")
    )
    this.bookRegistry.bookOrException(
      BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f")
    )
    this.bookRegistry.bookOrException(
      BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113")
    )

    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      0
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED) }
    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      1
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED) }
    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      2
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED) }
  }

  /**
   * If the remote side returns few books than the account has, database entries are removed.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksSyncRemoveEntries() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    /*
     * Populate the database by syncing against a feed that contains books.
     */

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(resource("testBooksSyncNewEntries.xml")))
    )

    controller.booksSync(account).get()

    this.bookRegistry.bookOrException(
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")
    )
    this.bookRegistry.bookOrException(
      BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f")
    )
    this.bookRegistry.bookOrException(
      BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113")
    )

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })

    /*
     * Now run the sync again but this time with a feed that removes books.
     */

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(resource("testBooksSyncRemoveEntries.xml")))
    )

    controller.booksSync(account).get()
    Assert.assertEquals(1L, this.bookRegistry.books().size.toLong())

    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      0
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED) }
    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      1
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_REMOVED) }
    EventAssertions.isTypeAndMatches(
      BookStatusEvent::class.java,
      this.bookEvents,
      2
    ) { e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_REMOVED) }

    this.bookRegistry.bookOrException(
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")
    )

    checkBookIsNotInRegistry("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f")
    checkBookIsNotInRegistry("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113")
  }

  private fun checkBookIsNotInRegistry(id: String) {
    try {
      this.bookRegistry.bookOrException(BookID.create(id))
      Assert.fail("Book should not exist!")
    } catch (e: NoSuchElementException) {
      // Correctly raised
    }
  }

  /**
   * Deleting a book works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksDelete() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(resource("testBooksDelete.xml")))
    )

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    Assert.assertFalse(
      "Book must not have a saved EPUB file",
      this.bookRegistry.bookOrException(bookId)
        .book
        .isDownloaded
    )

    /*
     * Manually reach into the database and create a book in order to have something to delete.
     */

    run {
      val databaseEntry = account.bookDatabase.entry(bookId)

      //      databaseEntry.writeEPUB(File.createTempFile("book", ".epub"));
      //      this.bookRegistry.update(
      //          BookWithStatus.create(
      //              databaseEntry.book(), BookStatus.fromBook(databaseEntry.book())));
    }

    //    final OptionType<File> createdFile =
    //        this.bookRegistry.bookOrException(bookId).book().file();
    //    Assert.assertTrue(
    //        "Book must have a saved EPUB file",
    //        createdFile.isSome());
    //
    //    final File file = ((Some<File>) createdFile).get();
    //    Assert.assertTrue("EPUB must exist", file.isFile());

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })
    controller.bookDelete(account, bookId).get()

    Assert.assertTrue(
      "Book must not have a saved EPUB file",
      this.bookRegistry.book(bookId).isNone
    )

    // Assert.assertFalse("EPUB must not exist", file.exists());
  }

  /**
   * Dismissing a failed revocation that didn't actually fail does nothing.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeDismissHasNotFailed() {
    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        http = this.lsHTTP,
        books = this.bookRegistry,
        profiles = this.profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders(),
        patronUserProfileParsers = this.patronUserProfileParsers
      )

    val provider =
      MockAccountProviders.fakeAuthProvider(
        uri = "urn:fake-auth:0",
        host = this.server.hostName,
        port = this.server.port
      )

    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id)
    val account = profile.accountsByProvider()[provider.id]!!
    account.setLoginState(AccountLoggedIn(correctCredentials()))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(resource("testBooksSyncNewEntries.xml")))
    )

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    val statusBefore = this.bookRegistry.bookOrException(bookId).status
    Assert.assertThat(statusBefore, IsInstanceOf.instanceOf(BookStatus.Loaned.LoanedNotDownloaded::class.java))

    controller.bookRevokeFailedDismiss(account, bookId).get()

    val statusAfter = this.bookRegistry.bookOrException(bookId).status
    Assert.assertEquals(statusBefore, statusAfter)
  }

  private fun resource(file: String): InputStream {
    return BooksControllerContract::class.java.getResourceAsStream(file)!!
  }

  @Throws(IOException::class)
  private fun resourceSize(file: String): Long {
    var total = 0L
    val buffer = ByteArray(8192)
    resource(file).use { stream ->
      while (true) {
        val r = stream.read(buffer)
        if (r <= 0) {
          break
        }
        total += r.toLong()
      }
    }
    return total
  }

  @Throws(ProfileDatabaseException::class)
  private fun profilesDatabaseWithoutAnonymous(
    accountEvents: PublishSubject<AccountEvent>,
    dirProfiles: File
  ): ProfilesDatabaseType {
    return ProfilesDatabases.openWithAnonymousProfileDisabled(
      context(),
      this.analytics,
      accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialsStore,
      AccountsDatabases,
      dirProfiles
    )
  }

  private fun onAccountResolution(
    id: URI,
    message: String
  ) {
    this.logger.debug("resolution: {}: {}", id, message)
  }
}
