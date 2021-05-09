package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.Option
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.mockito.internal.verification.Times
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
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
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.mocking.MockCrashingFeedLoader
import org.nypl.simplified.tests.mocking.MockRevokeStringResources
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Contract for the `BookRevokeTask` class that doesn't involve DRM.
 */

class BookRevokeTaskTest {

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  val profileID =
    ProfileID(UUID.fromString("06fa7899-658a-4480-a796-ebf2ff00d5ec"))

  private val logger: Logger =
    LoggerFactory.getLogger(BookRevokeTaskTest::class.java)

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
  private lateinit var http: LSHTTPClientType
  private lateinit var server: MockWebServer

  private val bookRevokeStrings = MockRevokeStringResources()

  @BeforeEach
  @Throws(Exception::class)
  fun setUp() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration(
            applicationName = "simplified-tests",
            applicationVersion = "1.0.0",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

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

    this.server = MockWebServer()
    this.server.start()
  }

  @AfterEach
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorTimer.shutdown()
    this.server.close()
  }

  private fun createFeedLoader(executorFeeds: ListeningExecutorService): FeedLoaderType {
    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser()
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val searchParser =
      OPDSSearchParser.newParser()
    val transport =
      FeedHTTPTransport(this.http)

    val feedLoader =
      FeedLoader.create(
        bookFormatSupport = this.bookFormatSupport,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        contentResolver = this.contentResolver,
        exec = executorFeeds,
        parser = parser,
        searchParser = searchParser,
        transport = transport
      )

    feedLoader.showOnlySupportedBooks = false
    return feedLoader
  }

  /**
   * A loan that doesn't require DRM and has no revocation URI, trivially succeeds revocation.
   */

  @Test
  fun testRevokeNothing() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * If the book database deletion crashes, the revocation fails.
   */

  @Test
  fun testRevokeNothingDatabaseCrash0() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
        accountID = accountID,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    assertEquals(
      "I/O error",
      result.steps.last().resolution.exception!!.message
    )
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * If the book database entry crashes, the revocation fails.
   */

  @Test
  fun testRevokeNothingDatabaseCrash1() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookId =
      BookID.create("a")

    this.logBookEventsFor(bookId)

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .then {
        throw IOException("I/O error")
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    assertEquals("I/O error", result.steps.last().resolution.exception!!.message)
    assertEquals(0, this.server.requestCount)
  }

  /**
   * If the book database crashes when trying to update the book entry, the revocation fails.
   */

  @Test
  fun testRevokeDatabaseCrash2() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="https://example.com/Open-Access" type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Failure

    assertEquals("I/O error", result.steps.last().resolution.exception!!.message)

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIOpenAccess() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
      )
    opdsEntryBuilder.addAcquisition(acquisition)
    val opdsEntry = opdsEntryBuilder.build()

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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="https://example.com/Open-Access" type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIHeldReady() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
          Option.some(this.server.url("revoke").toUri())
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="http://example.com/HeldReady"
          type="application/atom+xml;relation=entry;profile=opds-catalog"
          rel="http://opds-spec.org/acquisition/borrow">
      <opds:indirectAcquisition type="application/vnd.adobe.adept+xml">
        <opds:indirectAcquisition type="application/epub+zip"/>
      </opds:indirectAcquisition>
      <opds:availability
        status="ready"
        since="2020-01-01T00:00:00Z"/>
      <opds:holds total="0"/>
      <opds:copies available="0" total="1"/>
    </link>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds trivially if no URI is provided.
   */

  @Test
  fun testRevokeURIHeldReadyWithoutURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIHeld() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
          Option.some(this.server.url("revoke").toUri())
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="http://example.com/Held-Indefinite"
          type="application/atom+xml;relation=entry;profile=opds-catalog"
          rel="http://opds-spec.org/acquisition">
      <opds:indirectAcquisition type="application/vnd.adobe.adept+xml">
        <opds:indirectAcquisition type="application/epub+zip"/>
      </opds:indirectAcquisition>
      <opds:availability
        status="reserved"
        since="2000-01-01T00:00:00Z"/>
      <opds:holds total="0"/>
      <opds:copies available="0" total="1"/>
    </link>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI trivially succeeds if no URI is provided.
   */

  @Test
  fun testRevokeURIHeldNoURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURILoaned() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
          Option.some(this.server.url("revoke").toUri())
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link
        href="https://example.com/borrow"
        type="application/atom+xml;relation=entry;profile=opds-catalog"
        rel="http://opds-spec.org/acquisition">
      <opds:indirectAcquisition type="application/vnd.adobe.adept+xml">
        <opds:indirectAcquisition type="application/epub+zip"/>
      </opds:indirectAcquisition>
      <opds:availability
          status="available"
          since="2000-01-01T00:00:00Z"/>
      <opds:holds total="0"/>
      <opds:copies available="1" total="1"/>
    </link>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds trivially if no URI is provided.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURILoanedNoURI() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI succeeds if the server returns the expected data.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIRevoked() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityRevoked.get(this.server.url("revoke").toUri())
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link
        href="https://example.com/borrow"
        type="application/atom+xml;relation=entry;profile=opds-catalog"
        rel="http://opds-spec.org/acquisition">
      <opds:indirectAcquisition type="application/vnd.adobe.adept+xml">
        <opds:indirectAcquisition type="application/epub+zip"/>
      </opds:indirectAcquisition>
      <opds:availability
          status="available"
          since="2000-01-01T00:00:00Z"/>
      <opds:holds total="0"/>
      <opds:copies available="1" total="1"/>
    </link>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI fails if the server returns a corrupted feed.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIFeedCorrupt() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server returns a NOT AUTHORIZED error.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIFeed401() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
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
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFuture(feedResult as FeedLoaderResult)))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * Revoking a book using a URI fails if the server times out.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIFeedTimeout() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(SettableFuture.create()))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
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
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  private class UniqueException : Exception()

  /**
   * Revoking a book using a URI fails if the feed loader crashes.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIFeedCrash() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    this.feedLoader = MockCrashingFeedLoader()

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
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
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    assertEquals(
      IOException::class.java,
      result.steps.last().resolution.exception!!.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Revoking a book using a URI fails if the server returns a feed with groups.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeURIFeedWithGroups() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        this.server.url("0.feed").toUri(),
        this.mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.resource("/org/nypl/simplified/tests/opds/acquisition-groups-0.xml"))
    )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    result as TaskResult.Failure
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * You can't revoke a holdable book.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeHoldable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFailedFuture(UniqueException())))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
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
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    assertEquals("notRevocable", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * You can't revoke a loanable book.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testRevokeLoanable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)

    val feedLoader =
      Mockito.mock(FeedLoaderType::class.java)

    Mockito.`when`(
      feedLoader.fetchURI(
        account = this.anyNonNull(),
        uri = this.anyNonNull(),
        auth = this.anyNonNull(),
        method = this.anyNonNull()
      )
    ).thenReturn(FluentFuture.from(Futures.immediateFailedFuture(UniqueException())))

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
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
    assertEquals(
      BookStatus.FailedRevoke::class.java,
      this.bookRegistry.bookOrException(bookId).status.javaClass
    )
    assertEquals("notRevocable", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Revoking an audio book using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIAudioBook() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="https://example.com/Open-Access" type="application/audiobook+json" rel="http://opds-spec.org/acquisition/open-access"/>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
  }

  /**
   * Revoking a PDF using a URI succeeds if the server returns the expected data.
   */

  @Test
  fun testRevokeURIPDF() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
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
        OPDSAvailabilityOpenAccess.get(Option.some(this.server.url("revoke").toUri()))
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
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <id>436974f2-03ac-4023-8618-84291c6795b7</id>
  <title>436974f2-03ac-4023-8618-84291c6795b7</title>
  <updated>2020-01-01T00:00:00Z</updated>

  <entry>
    <id>15626ed2-ecd7-43ec-8de2-0eb9be2f8c6a</id>
    <title>Open-Access</title>
    <summary type="html"/>
    <updated>2020-01-01T00:00:00Z</updated>
    <published>2020-01-01T00:00:00Z</published>
    <link href="https://example.com/Open-Access" type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
  </entry>
</feed>
          """.trimIndent()
        )
    )

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Success

    assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()

    assertEquals(1, this.server.requestCount)
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

  private fun resource(file: String): Buffer {
    val buffer = Buffer()
    buffer.readFrom(BookRevokeTaskTest::class.java.getResourceAsStream(file)!!)
    return buffer
  }

  private fun mimeOf(name: String): MIMEType {
    return MIMEParser.parseRaisingException(name)
  }
}
