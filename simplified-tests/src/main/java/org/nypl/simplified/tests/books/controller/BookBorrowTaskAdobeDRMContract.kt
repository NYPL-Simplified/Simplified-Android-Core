package org.nypl.simplified.tests.books.controller

import android.content.ContentResolver
import android.content.Context
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentListenerType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptProcedureType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMDeviceNotActive
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedContentType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedSystem
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.BookBorrowTask
import org.nypl.simplified.books.controller.BookTaskRequiredServices
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.clock.Clock
import org.nypl.simplified.clock.ClockType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.http.MockingHTTP
import org.nypl.simplified.tests.strings.MockBorrowStringResources
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Contract for the `BookBorrowTask` class that involves Adobe DRM.
 */

abstract class BookBorrowTaskAdobeDRMContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract val logger: Logger

  private lateinit var adeptConnector: AdobeAdeptConnectorType
  private lateinit var adeptExecutor: AdobeAdeptExecutorType
  private lateinit var audioBookManifestStrategies: AudioBookManifestStrategiesType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var booksDirectory: File
  private lateinit var bookTaskRequiredServices: BookTaskRequiredServices
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var cacheDirectory: File
  private lateinit var clock: () -> Instant
  private lateinit var contentResolver: ContentResolver
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var downloader: DownloaderType
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var executorDownloads: ListeningExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var http: MockingHTTP
  private lateinit var profile: ProfileType
  private lateinit var profilesDatabase: ProfilesDatabaseType
  private lateinit var services: MutableServiceDirectory
  private lateinit var tempDirectory: File

  private val bookBorrowStrings = MockBorrowStringResources()

  private val adobeCredentialsValid =
    AccountAuthenticationAdobePreActivationCredentials(
      vendorID = AdobeVendorID("OmniConsumerProducts"),
      clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
      deviceManagerURI = URI("https://example.com/devices"),
      postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
        deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
        userID = AdobeUserID("someone")
      )
    )

  private val accountCredentialsValid =
    AccountAuthenticationCredentials.Basic(
      userName = AccountUsername("user"),
      password = AccountPassword("password"),
      adobeCredentials = this.adobeCredentialsValid,
      authenticationDescription = null
    )

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
    this.bookEvents = Collections.synchronizedList(ArrayList())
    this.bookRegistry = BookRegistry.create()
    this.bundledContent =
      BundledContentResolverType { uri -> throw FileNotFoundException("missing") }
    this.contentResolver = Mockito.mock(ContentResolver::class.java)
    this.downloader =
      DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    this.feedLoader = this.createFeedLoader(this.executorFeeds)

    this.tempDirectory = TestDirectories.temporaryDirectory()
    this.cacheDirectory = File(this.tempDirectory, "cache")
    this.booksDirectory = File(this.tempDirectory, "books")

    this.cacheDirectory.mkdirs()
    this.booksDirectory.mkdirs()
    Preconditions.checkState(this.cacheDirectory.isDirectory)
    Preconditions.checkState(this.booksDirectory.isDirectory)

    this.clock = { Instant.now() }

    this.audioBookManifestStrategies =
      Mockito.mock(AudioBookManifestStrategiesType::class.java)
    this.adeptConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)
    this.adeptExecutor =
      Mockito.mock(AdobeAdeptExecutorType::class.java)
    this.profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    this.profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(this.profilesDatabase.currentProfileUnsafe())
      .thenReturn(this.profile)

    this.services = MutableServiceDirectory()
    this.services.putService(AdobeAdeptExecutorType::class.java, this.adeptExecutor)
    this.services.putService(
      AudioBookManifestStrategiesType::class.java,
      this.audioBookManifestStrategies
    )
    this.services.putService(BookBorrowStringResourcesType::class.java, this.bookBorrowStrings)
    this.services.putService(BookRegistryReadableType::class.java, this.bookRegistry)
    this.services.putService(BookRegistryType::class.java, this.bookRegistry)
    this.services.putService(BundledContentResolverType::class.java, this.bundledContent)
    this.services.putService(ClockType::class.java, Clock)
    this.services.putService(DownloaderType::class.java, this.downloader)
    this.services.putService(FeedLoaderType::class.java, this.feedLoader)
    this.services.putService(HTTPType::class.java, this.http)
    this.services.putService(ProfilesDatabaseType::class.java, this.profilesDatabase)

    this.bookTaskRequiredServices =
      BookTaskRequiredServices.createFromServices(this.contentResolver, this.services)
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorDownloads.shutdown()
    this.executorTimer.shutdown()
  }

  private fun createBookDatabase(): BookDatabaseType {
    val context =
      Mockito.mock(Context::class.java)

    return BookDatabase.open(
      context = context,
      parser = OPDSJSONParser.newParser(),
      serializer = OPDSJSONSerializer.newSerializer(),
      owner = this.accountID,
      directory = this.booksDirectory
    )
  }

  private fun createFeedLoader(executorFeeds: ListeningExecutorService): FeedLoaderType {
    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val searchParser =
      OPDSSearchParser.newParser()
    val transport =
      org.nypl.simplified.feeds.api.FeedHTTPTransport.newTransport(this.http)

    return FeedLoader.create(
      exec = executorFeeds,
      parser = parser,
      searchParser = searchParser,
      transport = transport,
      bookRegistry = this.bookRegistry,
      bundledContent = this.bundledContent,
      contentResolver = this.contentResolver
    )
  }

  /**
   * Borrowing an epub via an ACSM works if the connector says it has.
   */

  @Test // (timeout = 5_000L)
  fun testBorrowFeedACSMForEPUB() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      this.createBookDatabase()

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)

    val adobeCredentials =
      this.adobeCredentialsValid

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("password"),
        adobeCredentials = adobeCredentials,
        authenticationDescription = null
      )

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val file =
      File.createTempFile("borrow-test", ".epub")
    val loan =
      AdobeAdeptLoan(AdobeLoanID("abcd"), ByteBuffer.allocate(32), false)

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(
      this.adeptConnector.fulfillACSM(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptFulfillmentListenerType
      receiver.onFulfillmentProgress(0.0)
      receiver.onFulfillmentProgress(10.0)
      receiver.onFulfillmentProgress(20.0)
      receiver.onFulfillmentProgress(30.0)
      receiver.onFulfillmentProgress(40.0)
      receiver.onFulfillmentProgress(50.0)
      receiver.onFulfillmentProgress(60.0)
      receiver.onFulfillmentProgress(70.0)
      receiver.onFulfillmentProgress(80.0)
      receiver.onFulfillmentProgress(90.0)
      receiver.onFulfillmentProgress(100.0)
      receiver.onFulfillmentSuccess(file, loan)
      Unit
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.none(),
        listOf(
          OPDSIndirectAcquisition(
            mimeOf("application/vnd.adobe.adept+xml"),
            listOf(OPDSIndirectAcquisition(mimeOf("application/epub+zip"), listOf()))
          )
        )
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    val formatHandle =
      bookDatabase.entry(bookId).findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)!!

    Assert.assertEquals(loan, (formatHandle.format.drmInformation as BookDRMInformation.ACS).rights!!.second)
    Assert.assertNotEquals(null, formatHandle.format.file)
  }

  /**
   * Borrowing an epub via an ACSM fails if no DRM support is available.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMForEPUBUnavailableDRM() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    val adobeCredentials =
      this.adobeCredentialsValid

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("password"),
        adobeCredentials = adobeCredentials,
        authenticationDescription = null
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    this.services.ensureServiceIsNotPresent(AdobeAdeptExecutorType::class.java)

    this.bookTaskRequiredServices =
      BookTaskRequiredServices.createFromServices(this.contentResolver, this.services)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val errorData =
      results.errors().last() as DRMUnsupportedSystem

    Assert.assertEquals("Adobe ACS", errorData.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the resulting file is not an EPUB.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMForNonEPUB() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)

    val adobeCredentials =
      this.adobeCredentialsValid

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("password"),
        adobeCredentials = adobeCredentials,
        authenticationDescription = null
      )

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val file =
      File.createTempFile("borrow-test", ".epub")
    val loan =
      AdobeAdeptLoan(AdobeLoanID("abcd"), ByteBuffer.allocate(32), false)

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(
      this.adeptConnector.fulfillACSM(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptFulfillmentListenerType
      receiver.onFulfillmentProgress(0.0)
      receiver.onFulfillmentProgress(10.0)
      receiver.onFulfillmentProgress(20.0)
      receiver.onFulfillmentProgress(30.0)
      receiver.onFulfillmentProgress(40.0)
      receiver.onFulfillmentProgress(50.0)
      receiver.onFulfillmentProgress(60.0)
      receiver.onFulfillmentProgress(70.0)
      receiver.onFulfillmentProgress(80.0)
      receiver.onFulfillmentProgress(90.0)
      receiver.onFulfillmentProgress(100.0)
      receiver.onFulfillmentSuccess(file, loan)
      Unit
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token-pdf.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token-pdf.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMUnsupportedContentType

    Assert.assertEquals("Adobe ACS", error.system)
    Assert.assertEquals(mimeOf("application/pdf"), error.contentType)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the device is not active.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMDeviceNotActive() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)

    val adobeCredentials =
      AccountAuthenticationAdobePreActivationCredentials(
        vendorID = AdobeVendorID("OmniConsumerProducts"),
        clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
        deviceManagerURI = URI("https://example.com/devices"),
        postActivationCredentials = null
      )

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("password"),
        adobeCredentials = adobeCredentials,
        authenticationDescription = null
      )

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token-pdf.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token-pdf.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMDeviceNotActive

    Assert.assertEquals("Adobe ACS", error.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the ACSM is unreadable.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMUnparseable() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(accountCredentialsValid))

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/4096.bin"),
        this.resourceSize("/org/nypl/simplified/tests/books/4096.bin"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMError.DRMUnparseableACSM

    Assert.assertEquals("Adobe ACS", error.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if download is cancelled.
   */

  @Test(timeout = 5000L)
  fun testBorrowFeedACSMCancellation() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(accountCredentialsValid))

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(
      this.adeptConnector.fulfillACSM(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptFulfillmentListenerType
      receiver.onFulfillmentCancelled()
      Unit
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the connector raises an error code.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMFailsErrorCode() {

    val bookDatabase =
      this.createBookDatabase()
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(accountCredentialsValid))

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(
      this.adeptConnector.fulfillACSM(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptFulfillmentListenerType
      receiver.onFulfillmentFailure("E_TYPICAL")
      Unit
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val headers =
      mapOf(Pair("Content-Type", listOf("application/vnd.adobe.adept+xml")))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/adobe-token.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/adobe-token.xml"),
        headers,
        0L
      )
    )

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L
      )
    )

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some(mimeOf("application/vnd.adobe.adept+xml")),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    val task =
      BookBorrowTask(
        accountId = account.id,
        acquisition = acquisition,
        bookId = bookId,
        cacheDirectory = this.cacheDirectory,
        downloads = ConcurrentHashMap(),
        entry = opdsEntry,
        services = this.bookTaskRequiredServices
      )

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMError.DRMFailure

    Assert.assertEquals("Adobe ACS", error.system)
    Assert.assertEquals("E_TYPICAL", error.errorCode)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatus.FailedDownload::class.java, bookWithStatus.status.javaClass)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  private fun logBookEventsFor(bookId: BookID) {
    this.bookRegistry.bookEvents().subscribe {
      this.bookRegistry.bookStatus(bookId).map_ { status ->
        this.logger.debug("status: {}", status)
      }
    }
  }

  private fun resource(file: String): InputStream {
    return BookBorrowTaskAdobeDRMContract::class.java.getResourceAsStream(file)
  }

  @Throws(IOException::class)
  private fun resourceSize(file: String): Long {
    var total = 0L
    val buffer = ByteArray(8192)
    this.resource(file).use { stream ->
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

  private fun mimeOf(name: String): MIMEType {
    return MIMEParser.parseRaisingException(name)
  }
}
