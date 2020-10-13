package org.nypl.simplified.tests.books.controller

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.Option
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.mockito.internal.verification.Times
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.BookRevokeTask
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MockRevokeStringResources
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Contract for the `BookRevokeTask` class that doesn't involve DRM.
 */

abstract class BookRevokeTaskContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract val logger: Logger

  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var cacheDirectory: File
  private lateinit var clock: () -> Instant
  private lateinit var contentResolver: ContentResolverType
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var http: MockingHTTP

  private val bookRevokeStrings = MockRevokeStringResources()

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.http = MockingHTTP()
    this.executorBooks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorTimer = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.bookEvents = Collections.synchronizedList(ArrayList())
    this.bookRegistry = BookRegistry.create()
    this.bundledContent =
      BundledContentResolverType { uri -> throw FileNotFoundException("missing") }
    this.bookFormatSupport = Mockito.mock(BookFormatSupportType::class.java)
    this.contentResolver = Mockito.mock(ContentResolverType::class.java)
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.feedLoader = this.createFeedLoader(this.executorFeeds)
    this.clock = { Instant.now() }
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorTimer.shutdown()
  }

  private fun createFeedLoader(executorFeeds: ListeningExecutorService): FeedLoaderType {
    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser()
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val searchParser =
      OPDSSearchParser.newParser()
    val transport =
      FeedHTTPTransport.newTransport(this.http)

    return FeedLoader.create(
      bookFormatSupport = this.bookFormatSupport,
      bookRegistry = this.bookRegistry,
      bundledContent = this.bundledContent,
      contentResolver = this.contentResolver,
      exec = executorFeeds,
      parser = parser,
      searchParser = searchParser,
      transport = transport
    )
  }

  /**
   * A loan that doesn't require DRM and has no revocation URI, trivially succeeds revocation.
   */

  @Test
  fun testRevokeNothing() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * If the book database deletion crashes, the revocation fails.
   */

  @Test
  fun testRevokeNothingDatabaseCrash0() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.delete())
      .then {
        throw IOException("I/O error")
      }

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      "I/O error",
      result.steps.last().resolution.exception!!.message
    )
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * If the book database entry crashes, the revocation fails.
   */

  @Test
  fun testRevokeNothingDatabaseCrash1() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .then {
        throw IOException("I/O error")
      }

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals("I/O error", result.steps.last().resolution.exception!!.message)
  }

  /**
   * If the book database crashes when trying to update the book entry, the revocation fails.
   */

  @Test
  fun testRevokeDatabaseCrash2() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.writeOPDSEntry(this.anyNonNull()))
      .then {
        throw IOException("I/O error")
      }

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Failure

    Assert.assertEquals("I/O error", result.steps.last().resolution.exception!!.message)

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIOpenAccess() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIHeldReady() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityHeldReady.get(
          Option.none(),
          Option.some(URI("http://www.example.com/revoke/0"))
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds trivially if no URI is provided.
   */

  @Test
  fun testRevokeURIHeldReadyWithoutURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityHeldReady.get(
          Option.none(),
          Option.none()
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIHeld() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityHeld.get(
          Option.none(),
          Option.none(),
          Option.none(),
          Option.some(URI("http://www.example.com/revoke/0"))
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI trivially succeeds if no URI is provided.
   */

  @Test
  fun testRevokeURIHeldNoURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityHeld.get(
          Option.none(),
          Option.none(),
          Option.none(),
          Option.none()
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURILoaned() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoaned.get(
          Option.none(),
          Option.none(),
          Option.some(URI("http://www.example.com/revoke/0"))
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds trivially if no URI is provided.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURILoanedNoURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoaned.get(
          Option.none(),
          Option.none(),
          Option.none()
        )
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIRevoked() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityRevoked.get(URI("http://www.example.com/revoke/0"))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server returns a corrupted feed.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIFeedCorrupt() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryCorrupt(account.id, bookId, IOException()))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server returns a NOT AUTHORIZED error.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIFeed401() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feedResult =
      FeedLoaderFailedAuthentication(
        problemReport = null,
        exception = IOException(),
        message = "Failed",
        attributesInitial = mapOf()
      )

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server times out.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIFeedTimeout() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(SettableFuture.create()))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings,
        revokeServerTimeoutDuration = Duration.standardSeconds(2L)
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  private class UniqueException : Exception()

  /**
   * Revoking a book using a URI fails if the feed loader crashes.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIFeedCrash() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFailedFuture(UniqueException())))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings,
        revokeServerTimeoutDuration = Duration.standardSeconds(5L)
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    Assert.assertEquals(
      UniqueException::class.java,
      result.steps.last().resolution.exception!!.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server returns a feed with groups.
   */

  @Test(timeout = 5_000L)
  fun testRevokeURIFeedWithGroups() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    /*
     * Do the tedious work necessary to produce a feed with groups.
     */

    val feedBuilder =
      OPDSAcquisitionFeed.newBuilder(
        URI.create("http://www.example.com/0.feed"),
        "x",
        DateTime.now(),
        "Title"
      )

    val feedEntryBuilder =
      OPDSAcquisitionFeedEntry
        .newBuilder(
          "x",
          "Title",
          DateTime.now(),
          OPDSAvailabilityOpenAccess.get(Option.none())
        )

    feedEntryBuilder.addGroup(URI.create("group"), "group")

    val feedEntry =
      feedEntryBuilder
        .build()

    feedBuilder.addEntry(feedEntry)

    val rawFeed =
      feedBuilder.build()
    val feed =
      Feed.fromAcquisitionFeed(
        accountId = account.id,
        feed = rawFeed,
        filter = { true },
        search = null
      ) as Feed.FeedWithGroups

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * You can't revoke a holdable book.
   */

  @Test(timeout = 5_000L)
  fun testRevokeHoldable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityHoldable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFailedFuture(UniqueException())))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings,
        revokeServerTimeoutDuration = Duration.standardSeconds(5L)
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    Assert.assertEquals("notRevocable", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * You can't revoke a loanable book.
   */

  @Test(timeout = 5_000L)
  fun testRevokeLoanable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/epub+zip"),
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

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFailedFuture(UniqueException())))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings,
        revokeServerTimeoutDuration = Duration.standardSeconds(5L)
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    Assert.assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    Assert.assertEquals("notRevocable", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking an audio book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIAudioBook() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleAudioBook::class.java)

    val bookId =
      BookID.create("a")

    val bookFormat =
      BookFormat.BookFormatAudioBook(
        manifest = null,
        position = null,
        contentType = BookFormats.audioBookGenericMimeTypes().first(),
        drmInformation = BookDRMInformation.None
      )

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/audiobook+json"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf(bookFormat)
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Revoking a PDF using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIPDF() {
    val account =
      Mockito.mock(AccountType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandlePDF::class.java)

    val bookId =
      BookID.create("a")

    val bookFormat =
      BookFormat.BookFormatPDF(
        lastReadLocation = null,
        file = null,
        contentType = BookFormats.pdfMimeTypes().first(),
        drmInformation = BookDRMInformation.None
      )

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        this.mimeOf("application/pdf"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(URI("http://www.example.com/revoke/0")))
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf(bookFormat)
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    val feed =
      Feed.empty(
        feedID = "x",
        feedSearch = null,
        feedTitle = "Title",
        feedURI = URI("http://www.example.com/0.feed"),
        feedFacets = listOf(),
        feedFacetGroups = mapOf()
      )

    feed.entriesInOrder.add(FeedEntry.FeedEntryOPDS(account.id, opdsEntry))

    val feedResult =
      FeedLoaderResult.FeedLoaderSuccess(feed)

    Mockito.`when`(
      feedLoader.fetchURIRefreshing(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        account = account,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    Assert.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
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

  private fun resource(file: String): InputStream? {
    return BookRevokeTaskContract::class.java.getResourceAsStream(file)
  }

  @Throws(IOException::class)
  private fun resourceSize(file: String): Long {
    var total = 0L
    val buffer = ByteArray(8192)
    this.resource(file)?.use { stream ->
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
