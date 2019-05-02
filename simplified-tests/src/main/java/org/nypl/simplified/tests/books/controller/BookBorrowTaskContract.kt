package org.nypl.simplified.tests.books.controller

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountLoginState
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.Book
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.BookDatabaseType
import org.nypl.simplified.books.book_database.BookEvent
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.controller.BookBorrowTask
import org.nypl.simplified.books.exceptions.BookBorrowExceptionBadBorrowFeed
import org.nypl.simplified.books.exceptions.BookBorrowExceptionNoCredentials
import org.nypl.simplified.books.exceptions.BookUnexpectedTypeException
import org.nypl.simplified.books.feeds.FeedHTTPTransport
import org.nypl.simplified.books.feeds.FeedHTTPTransportException
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.http.MockingHTTP
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException
import java.net.URI
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

open class BookBorrowTaskContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))
  
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
    this.feedLoader = createFeedLoader(this.executorFeeds)
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
      FeedHTTPTransport.newTransport(this.http)

    return FeedLoader.create(
      exec = executorFeeds,
      parser = parser,
      searchParser = searchParser,
      transport = transport,
      bookRegistry = this.bookRegistry,
      bundledContent = bundledContent)
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

    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    this.http.addResponse(
      "http://www.example.com/0.pdf",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.pdf"),
        resourceSize("/org/nypl/simplified/tests/books/empty.pdf"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/pdf"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    this.http.addResponse(
      "http://www.example.com/0.json",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.json"),
        resourceSize("/org/nypl/simplified/tests/books/empty.json"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/audiobook+json"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(bundledContent.resolve(URI.create("simplified-bundled:0.epub")))
      .thenReturn(resource("/org/nypl/simplified/tests/books/4096.bin"))
    Mockito.`when`(account.bookDatabase())
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
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(bundledContent.resolve(URI.create("simplified-bundled:0.epub")))
      .thenThrow(FileNotFoundException("Missing"))
    Mockito.`when`(account.bookDatabase())
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
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    val headers = HashMap<String, MutableList<String>>()
    headers["Content-Type"] = mutableListOf("application/epub+zip")

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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
  fun testBorrowFeedEPUBGarbageFails() {

    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "NOT FOUND",
        404,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
    val exception =
      optionUnsafe((bookWithStatus.status() as BookStatusDownloadFailed).error) as FeedHTTPTransportException
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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.requiresCredentials())
      .thenReturn(true)
    Mockito.`when`(account.loginState())
      .thenReturn(AccountLoginState.AccountNotLoggedIn)
    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
    val exception =
      optionUnsafe((bookWithStatus.status() as BookStatusDownloadFailed).error)
        as BookBorrowExceptionNoCredentials
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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        resourceSize("/org/nypl/simplified/tests/books/borrow-epub-0.xml"),
        HashMap(),
        0L))

    val headers = HashMap<String, MutableList<String>>()
    headers["Content-Type"] = mutableListOf("application/nonsense")

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
    val exception =
      optionUnsafe((bookWithStatus.status() as BookStatusDownloadFailed).error)
        as BookUnexpectedTypeException
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

    this.http.addResponse(
      "http://www.example.com/0.feed",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/feed-no-usable-acquisitions.xml"),
        resourceSize("/org/nypl/simplified/tests/books/feed-no-usable-acquisitions.xml"),
        HashMap(),
        0L))

    this.http.addResponse(
      "http://example.com/fulfill/0",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = this.feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
    val exception =
      optionUnsafe((bookWithStatus.status() as BookStatusDownloadFailed).error)
        as BookBorrowExceptionBadBorrowFeed
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

    val headers = HashMap<String, MutableList<String>>()
    headers.put("Content-Type", mutableListOf("application/vnd.librarysimplified.bearer-token+json"))
    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/bearer-token-0.json"),
        resourceSize("/org/nypl/simplified/tests/books/bearer-token-0.json"),
        headers,
        0L))

    this.http.addResponse(
      "http://www.example.com/1.epub",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

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

    val headers = HashMap<String, MutableList<String>>()
    headers.put("Content-Type", mutableListOf("application/vnd.librarysimplified.bearer-token+json"))
    this.http.addResponse(
      "http://www.example.com/0.epub",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/bearer-token-bad.json"),
        resourceSize("/org/nypl/simplified/tests/books/bearer-token-bad.json"),
        headers,
        0L))

    this.http.addResponse(
      "http://www.example.com/1.epub",
      HTTPResultOK(
        "OK",
        200,
        resource("/org/nypl/simplified/tests/books/empty.epub"),
        resourceSize("/org/nypl/simplified/tests/books/empty.epub"),
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
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf())

    Mockito.`when`(account.bookDatabase())
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findFormatHandleForContentType("application/epub+zip"))
      .thenReturn(formatHandle)

    val task =
      BookBorrowTask(
        adobeDRM = null,
        downloader = this.downloader,
        downloads = ConcurrentHashMap(),
        feedLoader = feedLoader,
        bundledContent = this.bundledContent,
        bookRegistry = this.bookRegistry,
        bookId = bookId,
        account = account,
        acquisition = acquisition,
        entry = opdsEntry)

    task.call()

    /*
     * Check that the download failed.
     */

    val bookWithStatus = (this.bookRegistry.book(bookId) as Some<BookWithStatus>).get()
    Assert.assertEquals(BookStatusDownloadFailed::class.java, bookWithStatus.status().javaClass)
  }

  private fun <T> optionUnsafe( opt: OptionType<T>): T {
    return if (opt is Some<T>) {
      opt.get()
    } else {
      throw IllegalStateException("Expected something, got nothing!")
    }
  }

  private fun resource(file: String): InputStream {
    return BookBorrowTaskContract::class.java.getResourceAsStream(file)
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
}