package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors

import com.io7m.jfunctional.FunctionType
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Unit

import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountBarcode
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountPIN
import org.nypl.simplified.books.accounts.AccountProvider
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription
import org.nypl.simplified.books.accounts.AccountProviderCollection
import org.nypl.simplified.books.accounts.AccountsDatabases
import org.nypl.simplified.books.analytics.AnalyticsLogger
import org.nypl.simplified.books.book_database.BookEvent
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookStatusLoaned
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoCredentials
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoURI
import org.nypl.simplified.books.feeds.FeedHTTPTransport
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.profiles.ProfileDatabaseException
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfilesDatabase
import org.nypl.simplified.books.profiles.ProfilesDatabaseType
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
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.books.BooksContract
import org.nypl.simplified.tests.books.MappedHTTP
import org.nypl.simplified.tests.books.reader.bookmarks.NullReaderBookmarkService
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.NoSuchElementException
import java.util.TreeMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BooksControllerContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorDownloads: ListeningExecutorService
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var http: MockingHTTP
  private lateinit var profileEvents: ObservableType<ProfileEvent>
  private lateinit var accountEvents: ObservableType<AccountEvent>
  private lateinit var profileEventsReceived: MutableList<ProfileEvent>
  private lateinit var accountEventsReceived: MutableList<AccountEvent>
  private lateinit var downloader: DownloaderType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var profiles: ProfilesDatabaseType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var executorTimer: ListeningExecutorService

  protected abstract fun context(): Context

  private fun fakeProvider(providerId: String): AccountProvider {
    return AccountProvider.builder()
      .setId(URI.create(providerId))
      .setDisplayName("Fake Library")
      .setSubtitle(Option.some("Imaginary books"))
      .setLogo(Option.some(URI.create("http://example.com/logo.png")))
      .setCatalogURI(URI.create("http://example.com/accounts0/feed.xml"))
      .setSupportEmail("postmaster@example.com")
      .setAnnotationsURI(Option.some(URI.create("http://example.com/accounts0/annotations")))
      .setPatronSettingsURI(Option.some(URI.create("http://example.com/accounts0/patrons/me")))
      .build()
  }

  private fun accountProviders(unit: Unit): AccountProviderCollection {
    return accountProviders()
  }

  private fun accountProviders(): AccountProviderCollection {
    val fake0 = fakeProvider("urn:fake:0")
    val fake1 = fakeProvider("urn:fake:1")
    val fake2 = fakeProvider("urn:fake:2")
    val fake3 = fakeAuthProvider("urn:fake-auth:0")

    val providers = TreeMap<URI, AccountProvider>()
    providers[fake0.id()] = fake0
    providers[fake1.id()] = fake1
    providers[fake2.id()] = fake2
    providers[fake3.id()] = fake3
    return AccountProviderCollection.create(fake0, providers)
  }

  private fun fakeAuthProvider(uri: String): AccountProvider {
    return fakeProvider(uri)
      .toBuilder()
      .setAuthentication(Option.some(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create(uri))
        .setPassCodeLength(4)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build()))
      .build()
  }

  private fun correctCredentials(): OptionType<AccountAuthenticationCredentials> {
    return Option.of(
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("1234"), AccountBarcode.create("abcd"))
        .build())
  }

  private fun createController(
    exec: ExecutorService,
    feedExecutor: ListeningExecutorService,
    accountEvents: ObservableType<AccountEvent>,
    profileEvents: ObservableType<ProfileEvent>,
    http: HTTPType,
    books: BookRegistryType,
    profiles: ProfilesDatabaseType,
    downloader: DownloaderType,
    accountProviders: FunctionType<Unit, AccountProviderCollection>,
    timerExec: ExecutorService): BooksControllerType {

    val parser = OPDSFeedParser.newParser(
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes()))
    val transport =
      FeedHTTPTransport.newTransport(http)

    val bundledContent =
      BundledContentResolverType { uri -> throw FileNotFoundException(uri.toString()) }

    val feedLoader =
      FeedLoader.create(
        exec = feedExecutor,
        parser = parser,
        searchParser = OPDSSearchParser.newParser(),
        transport = transport,
        bookRegistry = books,
        bundledContent = bundledContent)

    val analyticsDirectory =
      File("/tmp/simplye-android-tests")
    val analyticsLogger =
      AnalyticsLogger.create(analyticsDirectory)

    return Controller.create(
      exec = exec,
      accountEvents = accountEvents,
      profileEvents = profileEvents,
      http = http,
      feedParser = parser,
      feedLoader = feedLoader,
      downloader = downloader,
      profiles = profiles,
      analyticsLogger = analyticsLogger,
      bookRegistry = books,
      bundledContent = bundledContent,
      accountProviders = accountProviders,
      timerExecutor = timerExec,
      adobeDrm = null,
      readerBookmarkEvents = Observable.create())
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.http = MockingHTTP()
    this.executorDownloads = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorBooks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorTimer = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.profileEvents = Observable.create<ProfileEvent>()
    this.profileEventsReceived = Collections.synchronizedList(ArrayList())
    this.accountEvents = Observable.create<AccountEvent>()
    this.accountEventsReceived = Collections.synchronizedList(ArrayList())
    this.profiles = profilesDatabaseWithoutAnonymous(this.accountEvents, this.directoryProfiles)
    this.bookEvents = Collections.synchronizedList(ArrayList())
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultError(
        400,
        "BAD REQUEST",
        0L,
        HashMap(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none<HTTPProblemReport>()))

    this.expected.expect(ExecutionException::class.java)
    this.expected.expectCause(IsInstanceOf.instanceOf(IOException::class.java))
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    Assert.assertTrue(account.credentials().isSome)

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultError(
        401,
        "UNAUTHORIZED",
        0L,
        HashMap(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none<HTTPProblemReport>()))

    controller.booksSync(account).get()

    Assert.assertTrue(account.credentials().isNone)
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeProvider("urn:fake:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    Assert.assertTrue(account.credentials().isSome)
    controller.booksSync(account).get()
    Assert.assertTrue(account.credentials().isSome)
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)

    Assert.assertTrue(account.credentials().isNone)
    controller.booksSync(account).get()
    Assert.assertTrue(account.credentials().isNone)
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(byteArrayOf(0x23, 0x10, 0x39, 0x59)),
        4L,
        HashMap(),
        0L))

    this.expected.expect(ExecutionException::class.java)
    this.expected.expectCause(IsInstanceOf.instanceOf(OPDSParseException::class.java))
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksSyncNewEntries.xml"),
        resourceSize("testBooksSyncNewEntries.xml"),
        HashMap(),
        0L))

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })

    Assert.assertEquals(0L, this.bookRegistry.books().size.toLong())
    controller.booksSync(account).get()
    Assert.assertEquals(3L, this.bookRegistry.books().size.toLong())

    this.bookRegistry.bookOrException(
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"))
    this.bookRegistry.bookOrException(
      BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f"))
    this.bookRegistry.bookOrException(
      BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113"))

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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    /*
     * Populate the database by syncing against a feed that contains books.
     */

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksSyncNewEntries.xml"),
        resourceSize("testBooksSyncNewEntries.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    this.bookRegistry.bookOrException(
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"))
    this.bookRegistry.bookOrException(
      BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f"))
    this.bookRegistry.bookOrException(
      BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113"))

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })

    /*
     * Now run the sync again but this time with a feed that removes books.
     */

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksSyncRemoveEntries.xml"),
        resourceSize("testBooksSyncRemoveEntries.xml"),
        HashMap(),
        0L))

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
      BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"))

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
   * Revoking a book causes a request to be made to the revocation URI.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeCorrectURI() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI.xml"),
        resourceSize("testBooksRevokeCorrectURI.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "urn:book:0:revoke",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI_Response.xml"),
        resourceSize("testBooksRevokeCorrectURI_Response.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    this.bookRegistry.bookEvents().subscribe({ this.bookEvents.add(it) })
    controller.bookRevoke(account, BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")).get()

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

    checkBookIsNotInRegistry("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")
  }

  /**
   * Revoking a book without credentials fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeWithoutCredentials() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI.xml"),
        resourceSize("testBooksRevokeCorrectURI.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "urn:book:0:revoke",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI_Response.xml"),
        resourceSize("testBooksRevokeCorrectURI_Response.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()
    account.setCredentials(Option.none())

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    try {
      controller.bookRevoke(account, bookId).get()
      Assert.fail("Exception must be raised")
    } catch (e: ExecutionException) {
      Assert.assertThat<Throwable>(e.cause, IsInstanceOf.instanceOf(BookRevokeExceptionNoCredentials::class.java))
    }

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusRevokeFailed::class.java))
  }

  /**
   * Revoking a book that has no revocation URI fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeWithoutURI() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeWithoutURI.xml"),
        resourceSize("testBooksRevokeWithoutURI.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    try {
      controller.bookRevoke(account, bookId).get()
      Assert.fail("Exception must be raised")
    } catch (e: ExecutionException) {
      Assert.assertThat<Throwable>(e.cause, IsInstanceOf.instanceOf(BookRevokeExceptionNoURI::class.java))
    }

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusRevokeFailed::class.java))
  }

  /**
   * If the server returns an empty feed in response to a revocation, revocation fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeEmptyFeed() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI.xml"),
        resourceSize("testBooksRevokeCorrectURI.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "urn:book:0:revoke",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeEmptyFeed.xml"),
        resourceSize("testBooksRevokeEmptyFeed.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    try {
      controller.bookRevoke(account, bookId).get()
      Assert.fail("Exception must be raised")
    } catch (e: ExecutionException) {
      Assert.assertThat<Throwable>(e.cause, IsInstanceOf.instanceOf(IOException::class.java))
    }

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusRevokeFailed::class.java))
  }

  /**
   * If the server returns a garbage in response to a revocation, revocation fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeGarbage() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI.xml"),
        resourceSize("testBooksRevokeCorrectURI.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "urn:book:0:revoke",
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    try {
      controller.bookRevoke(account, bookId).get()
      Assert.fail("Exception must be raised")
    } catch (e: ExecutionException) {
      Assert.assertThat<Throwable>(e.cause, IsInstanceOf.instanceOf(IOException::class.java))
    }

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusRevokeFailed::class.java))
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksDelete.xml"),
        resourceSize("testBooksDelete.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    Assert.assertFalse(
      "Book must not have a saved EPUB file",
      this.bookRegistry.bookOrException(bookId)
        .book()
        .isDownloaded)

    /*
     * Manually reach into the database and create a book in order to have something to delete.
     */

    run {
      val databaseEntry = account.bookDatabase().entry(bookId)

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
      this.bookRegistry.book(bookId).isNone)

    // Assert.assertFalse("EPUB must not exist", file.exists());
  }

  /**
   * Dismissing a failed revocation works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  @Throws(Exception::class)
  fun testBooksRevokeDismissOK() {

    val controller =
      createController(
        exec = this.executorBooks,
        feedExecutor = this.executorFeeds,
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksRevokeCorrectURI.xml"),
        resourceSize("testBooksRevokeCorrectURI.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "urn:book:0:revoke",
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    try {
      controller.bookRevoke(account, bookId).get()
      Assert.fail("Exception must be raised")
    } catch (e: ExecutionException) {
      Assert.assertThat<Throwable>(e.cause, IsInstanceOf.instanceOf(IOException::class.java))
    }

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusRevokeFailed::class.java))

    controller.bookRevokeFailedDismiss(account, bookId).get()

    Assert.assertThat(
      this.bookRegistry.bookOrException(bookId).status(),
      IsInstanceOf.instanceOf(BookStatusLoaned::class.java))
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
        http = http,
        books = this.bookRegistry,
        profiles = this.profiles,
        downloader = this.downloader,
        accountProviders = FunctionType { accountProviders(it) },
        timerExec = this.executorTimer,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents)

    val provider = fakeAuthProvider("urn:fake-auth:0")
    val profile = this.profiles.createProfile(provider, "Kermit")
    this.profiles.setProfileCurrent(profile.id())
    val account = profile.createAccount(provider)
    account.setCredentials(correctCredentials())

    this.http.addResponse(
      "urn:fake-auth:0",
      HTTPResultOK(
        "OK",
        200,
        resource("testBooksSyncNewEntries.xml"),
        resourceSize("testBooksSyncNewEntries.xml"),
        HashMap(),
        0L))

    controller.booksSync(account).get()

    val bookId = BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")

    val statusBefore = this.bookRegistry.bookOrException(bookId).status()
    Assert.assertThat(statusBefore, IsInstanceOf.instanceOf(BookStatusLoaned::class.java))

    controller.bookRevokeFailedDismiss(account, bookId).get()

    val statusAfter = this.bookRegistry.bookOrException(bookId).status()
    Assert.assertEquals(statusBefore, statusAfter)
  }

  private fun resource(file: String): InputStream {
    return BooksControllerContract::class.java.getResourceAsStream(file)
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
    accountEvents: ObservableType<AccountEvent>,
    dirProfiles: File?): ProfilesDatabaseType {
    return ProfilesDatabase.openWithAnonymousAccountDisabled(
      context(),
      accountEvents,
      accountProviders(Unit.unit()),
      AccountsDatabases,
      dirProfiles)
  }


  companion object {
    private val LOG = LoggerFactory.getLogger(BooksContract::class.java)
    private val LOANS_URI = URI.create("http://example.com/loans/")
    private val ROOT_URI = URI.create("http://example.com/")

    private fun httpResource(name: String): MappedHTTP.MappedResource {
      val stream = BooksContract.javaClass.getResourceAsStream(name)
      ByteArrayOutputStream().use { outStream ->
        val size = stream.copyTo(outStream, 1024)
        val copyStream = ByteArrayInputStream(outStream.toByteArray())
        return MappedHTTP.MappedResource(copyStream, size)
      }
    }
  }
}
