package org.nypl.simplified.tests.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
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
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.HTTPRequestFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnsupportedAcquisition
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.BookBorrowTask
import org.nypl.simplified.books.controller.api.BookBorrowExceptionNoCredentials
import org.nypl.simplified.books.controller.api.BookUnexpectedTypeException
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedHTTPTransportException
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.strings.MockBorrowStringResources
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Contract for the `BookBorrowTask` class that doesn't involve DRM.
 */

abstract class BookBorrowTaskContract {

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
  private lateinit var cacheDirectory: File

  private val bookBorrowStrings = MockBorrowStringResources()

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
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.downloader = DownloaderHTTP.newDownloader(this.executorDownloads, this.directoryDownloads, this.http)
    this.feedLoader = this.createFeedLoader(this.executorFeeds)
    this.clock = { Instant.now() }
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
   * Borrowing an open access epub directly, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowOpenAccessEPUB() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Borrowing an open access PDF directly, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowOpenAccessPDF() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandlePDF::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.pdf",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.pdf"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.pdf"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.pdf"),
        Option.some("application/pdf"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/pdf"))
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
        feedLoader = feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Borrowing an open access audio book directly, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowOpenAccessAudioBook() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleAudioBook::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.json",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.json"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.json"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.json"),
        Option.some("application/audiobook+json"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/audiobook+json"))
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
        feedLoader = feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInManifestAndURI(
        File(this.directoryDownloads, "0000000000000001.data"),
        URI.create("http://www.example.com/0.json"))
  }

  /**
   * Borrowing bundled content works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowBundledEPUB() {

    val tempFile =
      File.createTempFile("nypl-test", "epub")
    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val bundledContent =
      Mockito.mock(BundledContentResolverType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("simplified-bundled:0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    Mockito.`when`(bundledContent.resolve(URI.create("simplified-bundled:0.epub")))
      .thenReturn(this.resource("/org/nypl/simplified/tests/books/4096.bin"))
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.temporaryFile())
      .thenReturn(tempFile)
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
        bundledContent = bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(tempFile)
  }

  /**
   * Borrowing bundled (missing) content fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowBundledEPUBMissing() {

    val tempFile =
      File.createTempFile("nypl-test", "epub")
    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val bundledContent =
      Mockito.mock(BundledContentResolverType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("simplified-bundled:0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    Mockito.`when`(bundledContent.resolve(URI.create("simplified-bundled:0.epub")))
      .thenThrow(FileNotFoundException("Missing"))
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.temporaryFile())
      .thenReturn(tempFile)
    Mockito.`when`(bookDatabaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        account = account,
        acquisition = acquisition,
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * Borrowing an epub via a feed, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUB() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Borrowing an epub via a generic acquisition, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBGeneric() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_GENERIC,
        URI.create("http://example.com/fulfill/0"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Borrowing an epub via a feed, works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBContentTypeExact() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    val headers = HashMap<String, MutableList<String>>()
    headers["Content-Type"] = mutableListOf("application/epub+zip")

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        headers,
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Borrowing an epub via a feed that turns out to be garbage, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedGarbageFails0() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing an epub via a feed that turns out to be garbage, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedGarbageFails1() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"))

    feed.entriesInOrder.add(
      FeedEntry.FeedEntryCorrupt(BookID.create("a"), java.lang.IllegalStateException()))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(feedLoader.fetchURIRefreshing(anyNonNull(), anyNonNull(), anyNonNull()))
      .thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * Borrowing an epub via a feed that turns out to be garbage, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedGarbageFails2() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    /*
     * Do the tedious work necessary to produce a feed with groups containing a corrupted
     * entry.
     */

    val feedBuilder =
      OPDSAcquisitionFeed.newBuilder(
        URI.create("http://www.example.com/0.feed"),
        "x",
        DateTime.now(),
        "Title")

    val feedEntryBuilder =
      OPDSAcquisitionFeedEntry
        .newBuilder(
          "x",
          "Title",
          DateTime.now(),
          OPDSAvailabilityOpenAccess.get(Option.none()))

    feedEntryBuilder.addGroup(URI.create("group"), "group")

    val feedEntry =
      feedEntryBuilder
        .build()

    feedBuilder.addEntry(feedEntry)

    val rawFeed =
      feedBuilder.build()
    val feed =
      Feed.fromAcquisitionFeed(rawFeed, null) as Feed.FeedWithGroups

    feed.feedGroupsInOrder[0].groupEntries[0] =
      FeedEntry.FeedEntryCorrupt(BookID.create("x"), java.lang.IllegalStateException())

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(feedLoader.fetchURIRefreshing(anyNonNull(), anyNonNull(), anyNonNull()))
      .thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * Borrowing an epub via a feed that turns out to be garbage, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedGarbageFails3() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(feedLoader.fetchURIRefreshing(anyNonNull(), anyNonNull(), anyNonNull()))
      .thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * Borrowing an epub via a feed that turns out to be garbage, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedGarbageFails4() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    /*
     * Do the tedious work necessary to produce a feed with groups containing a corrupted
     * entry.
     */

    val feedBuilder =
      OPDSAcquisitionFeed.newBuilder(
        URI.create("http://www.example.com/0.feed"),
        "x",
        DateTime.now(),
        "Title")

    val feedEntryBuilder =
      OPDSAcquisitionFeedEntry
        .newBuilder(
          "x",
          "Title",
          DateTime.now(),
          OPDSAvailabilityOpenAccess.get(Option.none()))

    feedEntryBuilder.addGroup(URI.create("group"), "group")

    val feedEntry =
      feedEntryBuilder
        .build()

    feedBuilder.addEntry(feedEntry)

    val rawFeed =
      feedBuilder.build()
    val feed =
      Feed.fromAcquisitionFeed(rawFeed, null) as Feed.FeedWithGroups

    feed.feedGroupsInOrder[0].groupEntries[0] =
      FeedEntry.FeedEntryCorrupt(BookID.create("x"), java.lang.IllegalStateException())

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(feedLoader.fetchURIRefreshing(anyNonNull(), anyNonNull(), anyNonNull()))
      .thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * If the feed loader crashes, the download fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBFeedLoaderCrashes() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(feedLoader.fetchURIRefreshing(anyNonNull(), anyNonNull(), anyNonNull()))
      .thenThrow(NullPointerException("Not really!"))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
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
   * Borrowing an epub via a feed that turns out to be missing, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUB404Fails() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "NOT FOUND",
        404,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing an epub via a feed that turns out to require credentials that weren't given, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUB401Fails() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val error =
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0,
        HashMap(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none())

    /*
     * XXX: The current HTTP transport implementation will erroneously retry
     * even in the face of 4xx errors. We publish a pile of errors here so that
     * each of the retry attempts will receive the same error. This bug will go
     * away when we switch to using a more modern HTTP library.
     */

    this.http.addResponse("http://www.example.com/0.feed", error)
    this.http.addResponse("http://www.example.com/0.feed", error)
    this.http.addResponse("http://www.example.com/0.feed", error)
    this.http.addResponse("http://www.example.com/0.feed", error)
    this.http.addResponse("http://www.example.com/0.feed", error)
    this.http.addResponse("http://www.example.com/0.feed", error)

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookStatus =
      (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get().status()
        as BookStatusDownloadFailed

    val exception =
      bookStatus.result.steps.last().resolution.exception as FeedHTTPTransportException
    Assert.assertEquals(401, exception.code)
  }

  /**
   * Borrowing an epub via a feed on an account that needs (missing) credentials, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBCredentialsNeededButMissing() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    Mockito.`when`(account.requiresCredentials)
      .thenReturn(true)
    Mockito.`when`(account.loginState)
      .thenReturn(org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn)
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

    /*
     * Check that the download failed.
     */

    val bookStatus =
      (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get().status()
        as BookStatusDownloadFailed

    val exception =
      bookStatus.result.steps.last().resolution.exception as BookBorrowExceptionNoCredentials
  }

  /**
   * Borrowing an epub via a feed from a server that delivers stupid content types, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBContentTypeNonsense() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    val headers = HashMap<String, MutableList<String>>()
    headers["Content-Type"] = mutableListOf("application/nonsense")

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        headers,
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookStatus =
      (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get().status()
        as BookStatusDownloadFailed

    val exception =
      bookStatus.result.steps.last().resolution.exception as BookUnexpectedTypeException
  }

  /**
   * Borrowing an epub via a feed that turns out not to have usable acquisitions, fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBNoUsableAcquisition() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/feed-only-buy-acquisitions.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/feed-only-buy-acquisitions.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookStatus =
      (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get().status()
        as BookStatusDownloadFailed

    val exception =
      bookStatus.result.steps.last().resolution.exception as IllegalStateException
  }

  /**
   * Borrowing a book with a bearer token works.
   */

  @Test(timeout = 5_000L)
  fun testBorrowBearerToken() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val headers = HashMap<String, MutableList<String>>()
    headers.put("Content-Type", mutableListOf("application/vnd.librarysimplified.bearer-token+json"))
    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/bearer-token-0.json"),
        this.resourceSize("/org/nypl/simplified/tests/books/bearer-token-0.json"),
        headers,
        0L))

    this.http.addResponse(
      "http://www.example.com/1.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader, http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000002.data"))
  }

  /**
   * Borrowing a book with an unparseable bearer token fails.
   */

  @Test(timeout = 5_000L)
  fun testBorrowBearerTokenUnparseable() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val headers = HashMap<String, MutableList<String>>()
    headers.put("Content-Type", mutableListOf("application/vnd.librarysimplified.bearer-token+json"))
    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/bearer-token-bad.json"),
        this.resourceSize("/org/nypl/simplified/tests/books/bearer-token-bad.json"),
        headers,
        0L))

    this.http.addResponse(
      "http://www.example.com/1.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
        adobeDRM = null,
        bookId = bookId,
        bookRegistry = this.bookRegistry,
        borrowStrings = this.bookBorrowStrings,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = this.clock,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
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
   * Cancelling a download fails the download!
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBCancelled() {

    this.downloader =
      Mockito.mock(DownloaderType::class.java)
    val download =
      Mockito.mock(DownloadType::class.java)

    Mockito.`when`(downloader.download(anyNonNull(), anyNonNull(), anyNonNull()))
      .then { invocation ->
        val listener = invocation.arguments[2] as DownloadListenerType
        listener.onDownloadStarted(download, 1000L)
        listener.onDownloadCancelled(download)
        download
      }

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
    Assert.assertEquals(
      CancellationException::class.java,
      results.steps.last().resolution.exception!!.javaClass)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * A failing download fails the borrow and gives a problem report.
   */

  @Test(timeout = 5_000L)
  fun testBorrowFeedEPUBDownloadFails() {

    this.downloader =
      Mockito.mock(DownloaderType::class.java)
    val download =
      Mockito.mock(DownloadType::class.java)

    val reportNode =
      ObjectMapper().createObjectNode()
    val report =
      HTTPProblemReport(reportNode)

    Mockito.`when`(downloader.download(anyNonNull(), anyNonNull(), anyNonNull()))
      .then { invocation ->
        val listener = invocation.arguments[2] as DownloadListenerType
        listener.onDownloadStarted(download, 1000L)
        listener.onDownloadFailed(download, 404, 1000L, Option.some(report), Option.none())
        download
      }

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        this.resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
      results.errors().last() as HTTPRequestFailed

    Assert.assertEquals(404, errorData.status)
    Assert.assertEquals(report, errorData.errorReport)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * It's not possible to borrow using unsupported acquisition types.
   */

  @Test(timeout = 5_000L)
  fun testBorrowUnsupportedAcquisition() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BUY,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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
      results.errors().last() as UnsupportedAcquisition

    Assert.assertEquals(ACQUISITION_BUY, errorData.type)

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * If the book database raises an exception, borrowing fails quickly.
   */

  @Test(timeout = 5_000L)
  fun testBorrowDatabaseFailure() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    Mockito.`when`(bookDatabase.createOrUpdate(anyNonNull(), anyNonNull()))
      .thenThrow(BookDatabaseException("Ouch", listOf()))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_BUY,
        URI.create("http://www.example.com/0.feed"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
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

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  /**
   * Borrowing a book with a cover results in fetching the cover.
   */

  @Test(timeout = 5_000L)
  fun testBorrowCoverOK() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://www.example.com/cover.jpg",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.setCoverOption(Option.some(URI.create("http://www.example.com/cover.jpg")))
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

    val tempCoverFile =
      File.createTempFile("borrow-contract", "jpg")

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.temporaryFile())
      .thenReturn(tempCoverFile)
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
        feedLoader = feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(bookDatabaseEntry, Mockito.times(1))
      .setCover(tempCoverFile)
    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
  }

  /**
   * Failing fetching a cover doesn't fail the borrowing as a whole.
   */

  @Test(timeout = 5_000L)
  fun testBorrowCoverFailure() {

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(account.id).thenReturn(this.accountID)

    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        this.resource("/org/nypl/simplified/tests/books/empty.epub"),
        this.resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://www.example.com/cover.jpg",
      HTTPResultError(
        400,
        "BAD!",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()))

    val acquisition =
      OPDSAcquisition(
        ACQUISITION_OPEN_ACCESS,
        URI.create("http://www.example.com/0.epub"),
        Option.some("application/epub+zip"),
        listOf())

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.setCoverOption(Option.some(URI.create("http://www.example.com/cover.jpg")))
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

    val tempCoverFile =
      File.createTempFile("borrow-contract", "jpg")

    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.temporaryFile())
      .thenReturn(tempCoverFile)
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
        feedLoader = feedLoader,
        http = this.http,
        entry = opdsEntry)

    val results = task.call(); TaskDumps.dump(logger, results)
    results as TaskResult.Success

    /*
     * Check that the book was saved to the database.
     */

    Mockito.verify(bookDatabaseEntry, Mockito.times(0))
      .setCover(tempCoverFile)
    Mockito.verify(formatHandle, Mockito.times(1))
      .setAdobeRightsInformation(null)
    Mockito.verify(formatHandle, Mockito.times(1))
      .copyInBook(File(this.directoryDownloads, "0000000000000001.data"))
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
    return BookBorrowTaskContract::class.java.getResourceAsStream(file)
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