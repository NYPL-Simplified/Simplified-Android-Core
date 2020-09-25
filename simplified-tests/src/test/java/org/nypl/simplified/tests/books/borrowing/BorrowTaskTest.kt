package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatus.Downloading
import org.nypl.simplified.books.book_registry.BookStatus.FailedLoan
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedDownloaded
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedNotDownloaded
import org.nypl.simplified.books.book_registry.BookStatus.RequestingLoan
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.borrowing.BorrowRequest
import org.nypl.simplified.books.borrowing.BorrowRequirements
import org.nypl.simplified.books.borrowing.BorrowSubtasks
import org.nypl.simplified.books.borrowing.BorrowTask
import org.nypl.simplified.books.borrowing.BorrowTaskType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.opdsAcquisitionFeedEntry
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.MockAudioBookManifestStrategies
import org.nypl.simplified.tests.MockBookDatabase
import org.nypl.simplified.tests.MockBookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.tests.MockBookFormatSupport
import org.nypl.simplified.tests.MockBorrowSubtaskDirectory
import org.nypl.simplified.tests.MockBundledContentResolver
import org.nypl.simplified.tests.MockContentResolver
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.TestDirectories
import org.slf4j.LoggerFactory
import java.io.File

class BorrowTaskTest {

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var audioBookManifestStrategies: MockAudioBookManifestStrategies
  private lateinit var book: Book
  private lateinit var bookDatabase: MockBookDatabase
  private lateinit var bookEvents: MutableList<BookStatusEvent>
  private lateinit var bookFormatSupport: MockBookFormatSupport
  private lateinit var bookID: BookID
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bookStates: MutableList<BookStatus>
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var cacheDirectory: File
  private lateinit var contentResolver: ContentResolverType
  private lateinit var httpClient: LSHTTPClientType
  private lateinit var opdsEmptyFeedEntry: OPDSAcquisitionFeedEntry
  private lateinit var opdsOpenEPUBFeedEntry: OPDSAcquisitionFeedEntry
  private lateinit var profile: ProfileReadableType
  private lateinit var services: MutableServiceDirectory
  private lateinit var subtasks: MockBorrowSubtaskDirectory
  private lateinit var temporaryDirectory: File
  private lateinit var webServer: MockWebServer
  private var bookRegistrySub: Disposable? = null

  private val logger = LoggerFactory.getLogger(BorrowTaskTest::class.java)

  private fun createTask(request: BorrowRequest): BorrowTaskType {
    this.book =
      Book(
        id = BookIDs.newFromOPDSEntry(request.opdsAcquisitionFeedEntry),
        account = request.accountId,
        cover = null,
        thumbnail = null,
        entry = request.opdsAcquisitionFeedEntry,
        formats = listOf()
      )

    return BorrowTask.createBorrowTask(
      requirements = BorrowRequirements(
        adobeExecutor = null,
        audioBookManifestStrategies = this.audioBookManifestStrategies,
        bookFormatSupport = this.bookFormatSupport,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        clock = { Instant.now() },
        contentResolver = this.contentResolver,
        httpClient = this.httpClient,
        profile = this.profile,
        subtasks = this.subtasks,
        temporaryDirectory = this.temporaryDirectory,
        services = this.services,
        cacheDirectory = this.cacheDirectory
      ),
      request = request
    )
  }

  private fun verifyBookRegistryHasStatus(clazz: Class<*>) {
    val registryStatus = this.bookRegistry.bookStatusOrNull(this.bookID)!!
    assertEquals(clazz, registryStatus.javaClass)
  }

  private fun executeAssumingFailure(task: BorrowTaskType): TaskResult.Failure<*> {
    val result = task.execute()
    result.steps.forEach { this.logger.debug("{}", it) }
    return result as TaskResult.Failure
  }

  private fun executeAssumingSuccess(task: BorrowTaskType): TaskResult.Success<*> {
    val result = task.execute()
    result.steps.forEach { this.logger.debug("{}", it) }
    return result as TaskResult.Success
  }

  @Before
  fun testSetup() {
    this.webServer = MockWebServer()
    this.webServer.start(20000)

    this.opdsEmptyFeedEntry =
      BorrowTests.opdsEmptyFeedEntryOfType()

    this.opdsOpenEPUBFeedEntry =
      BorrowTests.opdsOpenAccessFeedEntryOfType(
        this.webServer,
        genericEPUBFiles.fullType
      )

    this.accountId =
      AccountID.generate()
    this.bookID =
      BookIDs.newFromOPDSEntry(this.opdsOpenEPUBFeedEntry)

    this.bookDatabase =
      MockBookDatabase(this.accountId)

    this.bookRegistry =
      BookRegistry.create()
    this.bookStates =
      mutableListOf()
    this.bookEvents =
      mutableListOf()
    this.bookRegistrySub =
      this.bookRegistry.bookEvents()
        .subscribe(this::recordBookEvent)
    this.bookFormatSupport =
      MockBookFormatSupport()

    this.temporaryDirectory =
      TestDirectories.temporaryDirectory()
    this.cacheDirectory =
      TestDirectories.temporaryDirectory()
    this.audioBookManifestStrategies =
      MockAudioBookManifestStrategies()
    this.contentResolver =
      MockContentResolver()
    this.bundledContent =
      MockBundledContentResolver()
    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")
    this.services =
      MutableServiceDirectory()
    this.subtasks =
      MockBorrowSubtaskDirectory()

    val androidContext =
      Mockito.mock(Context::class.java)

    this.httpClient =
      LSHTTPClients()
        .create(
          context = androidContext,
          configuration = LSHTTPClientConfiguration(
            "simplified-tests",
            "999.999.0"
          )
        )

    this.subtasks.subtasks =
      BorrowSubtasks.directory()
        .subtasks

    Mockito.`when`(this.profile.account(this.accountId))
      .thenReturn(this.account)
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)
    Mockito.`when`(this.account.provider)
      .thenReturn(this.accountProvider)
  }

  @After
  fun tearDown() {
    this.bookRegistrySub?.dispose()
    this.webServer.close()
  }

  private fun recordBookEvent(event: BookStatusEvent) {
    this.logger.debug("event: {}", event)
    val status = this.bookRegistry.bookStatusOrNull(event.book())!!
    this.logger.debug("status: {}", status)
    this.bookStates.add(status)
    this.bookEvents.add(event)
  }

  /**
   * If the book database can't be set up, borrowing fails.
   */

  @Test
  fun testBrokenBookDatabase() {
    val request =
      BorrowRequest.Start(this.accountId, this.opdsEmptyFeedEntry)
    val task =
      this.createTask(request)

    Mockito.`when`(this.account.bookDatabase)
      .thenThrow(IllegalStateException("Book database on fire."))

    val result = this.executeAssumingFailure(task)
    assertEquals(BorrowErrorCodes.bookDatabaseFailed, result.lastErrorCode)

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
  }

  /**
   * If the account can't be found, borrowing fails.
   */

  @Test
  fun testNoAccount() {
    val request =
      BorrowRequest.Start(this.accountId, this.opdsEmptyFeedEntry)
    val task =
      this.createTask(request)

    Mockito.`when`(this.profile.account(this.accountId))
      .thenThrow(IllegalStateException("Missing account!"))

    val result = this.executeAssumingFailure(task)
    assertEquals(BorrowErrorCodes.accountsDatabaseException, result.lastErrorCode)

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
  }

  /**
   * A feed entry that provides no acquisitions can't be borrowed.
   */

  @Test
  fun testNoAvailableAcquisitions() {
    val request =
      BorrowRequest.Start(this.accountId, this.opdsEmptyFeedEntry)
    val task =
      this.createTask(request)

    val result = this.executeAssumingFailure(task)
    assertEquals(BorrowErrorCodes.noSupportedAcquisitions, result.lastErrorCode)

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
  }

  /**
   * An empty directory of subtasks causes all acquisitions to fail.
   */

  @Test
  fun testNoSubtasks() {
    val request =
      BorrowRequest.Start(this.accountId, this.opdsOpenEPUBFeedEntry)
    val task =
      this.createTask(request)

    this.subtasks.subtasks = listOf()

    val result = this.executeAssumingFailure(task)
    assertEquals(BorrowErrorCodes.noSubtaskAvailable, result.lastErrorCode)

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
  }

  /**
   * A simple direct EPUB download succeeds.
   */

  @Test
  fun testSimpleEPUB() {
    this.bookDatabase.entries.clear()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("A cold star looked down on his creations")
    )

    val request =
      BorrowRequest.Start(this.accountId, this.opdsOpenEPUBFeedEntry)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)
    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry = this.bookDatabase.entries[this.bookID]!!
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java) as MockBookDatabaseEntryFormatHandleEPUB

    assertEquals("A cold star looked down on his creations", handle.bookData)
  }

  /**
   * Creating a loan and then downloading an EPUB succeeds.
   */

  @Test
  fun testLoanEPUB() {
    this.bookDatabase.entries.clear()

    val loanable =
      BorrowTests.opdsLoanableIndirectFeedEntryOfType(this.webServer, genericEPUBFiles.fullType)
    val loaned =
      BorrowTests.opdsLoanedTextOfType(this.webServer, genericEPUBFiles.fullType)

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry.fullType)
        .setBody(loaned)
    )

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("A cold star looked down on his creations")
    )

    val request =
      BorrowRequest.Start(this.accountId, loanable)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry = this.bookDatabase.entries[this.bookID]!!
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java) as MockBookDatabaseEntryFormatHandleEPUB

    assertEquals("A cold star looked down on his creations", handle.bookData)
  }
}
