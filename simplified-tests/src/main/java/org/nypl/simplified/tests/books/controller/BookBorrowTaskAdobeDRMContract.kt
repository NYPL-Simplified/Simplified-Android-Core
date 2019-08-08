package org.nypl.simplified.tests.books.controller

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
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
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.BookBorrowTask
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.*
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMDeviceNotActive
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedContentType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedSystem
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.strings.MockBorrowStringResources
import org.nypl.simplified.tests.http.MockingHTTP
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

  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorDownloads: ListeningExecutorService
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var http: MockingHTTP
  private lateinit var downloader: DownloaderType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var clock: () -> Instant
  private lateinit var adeptExecutor: AdobeAdeptExecutorType
  private lateinit var adeptConnector: AdobeAdeptConnectorType
  private lateinit var cacheDirectory: File

  private val bookBorrowStrings = MockBorrowStringResources()

  private val adobeCredentialsValid =
    AccountAuthenticationAdobePreActivationCredentials(
      vendorID = AdobeVendorID("OmniConsumerProducts"),
      clientToken = AccountAuthenticationAdobeClientToken.create("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
      deviceManagerURI = URI("https://example.com/devices"),
      postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
        deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
        userID = AdobeUserID("someone")))

  private val accountCredentialsValid =
    AccountAuthenticationCredentials
      .builder(AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .setAdobeCredentials(this.adobeCredentialsValid)
      .build()

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
    this.bundledContent = BundledContentResolverType { uri -> throw FileNotFoundException("missing") }
    this.downloader = DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    this.feedLoader = this.createFeedLoader(this.executorFeeds)
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.clock = { Instant.now() }

    this.adeptConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)
    this.adeptExecutor =
      Mockito.mock(AdobeAdeptExecutorType::class.java)
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorDownloads.shutdown()
    this.executorTimer.shutdown()
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
      bundledContent = this.bundledContent)
  }

  /**
   * Borrowing an epub via an ACSM works if the connector says it has.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMForEPUB() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val adobeCredentials =
      this.adobeCredentialsValid

    val credentials =
      AccountAuthenticationCredentials
        .builder(AccountPIN.create("abcd"), AccountBarcode.create("1234"))
        .setAdobeCredentials(adobeCredentials)
        .build()

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val file =
      File.createTempFile("borrow-test", ".epub")
    val loan =
      AdobeAdeptLoan(AdobeLoanID("abcd"), ByteBuffer.allocate(32), false)

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(this.adeptConnector.fulfillACSM(
      this.anyNonNull(),
      this.anyNonNull(),
      this.anyNonNull()
    )).then { invocation ->
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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(loan)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(anyNonNull())
  }

  /**
   * Borrowing an epub via an ACSM fails if no DRM support is available.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMForEPUBUnavailableDRM() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val errorData =
      results.errors().last() as DRMUnsupportedSystem

    Assert.assertEquals("Adobe ACS", errorData.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the resulting file is not an EPUB.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMForNonEPUB() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val adobeCredentials =
      this.adobeCredentialsValid

    val credentials =
      AccountAuthenticationCredentials
        .builder(AccountPIN.create("abcd"), AccountBarcode.create("1234"))
        .setAdobeCredentials(adobeCredentials)
        .build()

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(credentials))

    val file =
      File.createTempFile("borrow-test", ".epub")
    val loan =
      AdobeAdeptLoan(AdobeLoanID("abcd"), ByteBuffer.allocate(32), false)

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(this.adeptConnector.fulfillACSM(
      this.anyNonNull(),
      this.anyNonNull(),
      this.anyNonNull()
    )).then { invocation ->
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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMUnsupportedContentType

    Assert.assertEquals("Adobe ACS", error.system)
    Assert.assertEquals("application/pdf", error.contentType)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the device is not active.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMDeviceNotActive() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val adobeCredentials =
      AccountAuthenticationAdobePreActivationCredentials(
        vendorID = AdobeVendorID("OmniConsumerProducts"),
        clientToken = AccountAuthenticationAdobeClientToken.create("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
        deviceManagerURI = URI("https://example.com/devices"),
        postActivationCredentials = null)

    val credentials =
      AccountAuthenticationCredentials
        .builder(AccountPIN.create("abcd"), AccountBarcode.create("1234"))
        .setAdobeCredentials(adobeCredentials)
        .build()

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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMDeviceNotActive

    Assert.assertEquals("Adobe ACS", error.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the ACSM is unreadable.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMUnparseable() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    val error =
      results.errors().last() as DRMError.DRMUnparseableACSM

    Assert.assertEquals("Adobe ACS", error.system)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if download is cancelled.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMCancellation() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(accountCredentialsValid))

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(this.adeptConnector.fulfillACSM(
      this.anyNonNull(),
      this.anyNonNull(),
      this.anyNonNull()
    )).then { invocation ->
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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Failure

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing something via an ACSM fails if the connector raises an error code.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedACSMFailsErrorCode() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountLoggedIn(accountCredentialsValid))

    /*
     * When the code calls fulfill(), it succeeds if the connector reports success.
     */

    Mockito.`when`(this.adeptConnector.fulfillACSM(
      this.anyNonNull(),
      this.anyNonNull(),
      this.anyNonNull()
    )).then { invocation ->
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
        0L))

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-acsm-epub-0.xml"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/vnd.adobe.adept+xml"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get())
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = this.adeptExecutor,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        http = this.http,
        entry = opdsEntry)

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
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  private fun logBookEventsFor(bookId: BookID?) {
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
}