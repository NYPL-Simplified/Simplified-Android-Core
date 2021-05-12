package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.drm.core.AxisNowFulfillment
import org.nypl.drm.core.AxisNowFulfillmentException
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatus.Downloading
import org.nypl.simplified.books.book_registry.BookStatus.FailedDownload
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedDownloaded
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.borrowing.internal.BorrowAxisNow
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.axisNowFulfillmentFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.axisNowNotSupported
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpContentTypeIncompatible
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpRequestFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames.axisNow
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockAxisNowService
import org.nypl.simplified.tests.mocking.MockBookDatabase
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntry
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.tests.mocking.MockBorrowContext
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockContentResolver
import org.nypl.simplified.tests.mocking.MockDRMInformationAxisHandle
import org.slf4j.LoggerFactory
import java.net.URI

class BorrowAxisNowTest {

  private val validToken =
    """{"book_vault_uuid": "14998ffa-f60b-4471-ad49-54602bd4ff11", "isbn": "9781626725232"}"""

  private val validTokenResponse =
    MockResponse()
      .setResponseCode(200)
      .setHeader("content-type", axisNow.fullType)
      .setBody(this.validToken)

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var axisHandle: MockDRMInformationAxisHandle
  private lateinit var axisNowService: MockAxisNowService
  private lateinit var bookDatabase: MockBookDatabase
  private lateinit var bookDatabaseEPUBHandle: MockBookDatabaseEntryFormatHandleEPUB
  private lateinit var bookDatabaseEntry: MockBookDatabaseEntry
  private lateinit var bookEvents: MutableList<BookStatusEvent>
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookID: BookID
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bookStates: MutableList<BookStatus>
  private lateinit var bundledContent: MockBundledContentResolver
  private lateinit var contentResolver: MockContentResolver
  private lateinit var context: MockBorrowContext
  private lateinit var httpClient: LSHTTPClientType
  private lateinit var profile: ProfileReadableType
  private lateinit var taskRecorder: TaskRecorderType
  private lateinit var webServer: MockWebServer
  private var bookRegistrySub: Disposable? = null
  private var onEvent: (BookStatusEvent) -> Unit = { }

  private val logger = LoggerFactory.getLogger(BorrowAxisNowTest::class.java)

  private fun verifyBookRegistryHasStatus(clazz: Class<*>) {
    val registryStatus = this.bookRegistry.bookStatusOrNull(this.bookID)!!
    assertEquals(clazz, registryStatus.javaClass)
  }

  @BeforeEach
  fun testSetup() {
    this.webServer = MockWebServer()
    this.webServer.start(20000)

    this.taskRecorder =
      TaskRecorder.create()
    this.contentResolver =
      MockContentResolver()
    this.bundledContent =
      MockBundledContentResolver()

    this.bookFormatSupport =
      Mockito.mock(BookFormatSupportType::class.java)
    this.bookRegistry =
      BookRegistry.create()
    this.bookEvents =
      mutableListOf()
    this.bookStates =
      mutableListOf()
    this.bookRegistrySub =
      this.bookRegistry.bookEvents()
        .subscribe(this::recordBookEvent)

    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    val initialFeedEntry =
      BorrowTestFeeds.opdsLoanedFeedEntryOfType(this.webServer, genericEPUBFiles.fullType)
    this.bookID =
      BookIDs.newFromOPDSEntry(initialFeedEntry)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")

    Mockito.`when`(this.account.loginState)
      .thenReturn(
        AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = null,
            authenticationDescription = "Basic",
            annotationsURI = URI("https://www.example.com")
          )
        )
      )

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

    this.accountId =
      AccountID.generate()

    val bookInitial =
      Book(
        id = this.bookID,
        account = this.accountId,
        cover = null,
        thumbnail = null,
        entry = initialFeedEntry,
        formats = listOf()
      )

    this.bookDatabase =
      MockBookDatabase(this.accountId)
    this.bookDatabaseEntry =
      MockBookDatabaseEntry(bookInitial)

    this.bookDatabaseEPUBHandle =
      MockBookDatabaseEntryFormatHandleEPUB(this.bookID)
    this.bookDatabaseEntry.formatHandlesField.clear()
    this.bookDatabaseEntry.formatHandlesField.add(this.bookDatabaseEPUBHandle)
    this.axisHandle =
      MockDRMInformationAxisHandle()
    this.bookDatabaseEPUBHandle.drmInformationHandleField =
      this.axisHandle

    this.axisNowService =
      MockAxisNowService()

    this.context =
      MockBorrowContext(
        logger = this.logger,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        temporaryDirectory = TestDirectories.temporaryDirectory(),
        account = this.account,
        clock = { Instant.now() },
        httpClient = this.httpClient,
        taskRecorder = this.taskRecorder,
        isCancelled = false,
        bookDatabaseEntry = this.bookDatabaseEntry,
        bookInitial = bookInitial,
        contentResolver = this.contentResolver
      )

    this.context.axisNowService = this.axisNowService
    this.context.opdsAcquisitionPath =
      OPDSAcquisitionPath(
        OPDSAcquisition(
          OPDSAcquisition.Relation.ACQUISITION_GENERIC,
          this.webServer.url("/book").toUri(),
          axisNow,
          listOf()
        ),
        listOf(
          OPDSAcquisitionPathElement(
            axisNow,
            this.webServer.url("/book").toUri()
          )
        )
      )

    this.context.currentRemainingOPDSPathElements =
      listOf()

    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(axisNow, null)
  }

  private fun recordBookEvent(event: BookStatusEvent) {
    this.logger.debug("event: {}", event)
    this.onEvent(event)
    val status = event.statusNow!!
    this.logger.debug("status: {}", status)
    this.bookStates.add(status)
    this.bookEvents.add(event)
  }

  @AfterEach
  fun tearDown() {
    this.bookRegistrySub?.dispose()
    this.webServer.close()
  }

  /**
   * If the application manages to get as far as this subtask without AxisNow support, then
   * the subtask fails with an appropriate "not supported" error code.
   */

  @Test
  fun testNotSupported() {
    this.context.axisNowService = null

    val task = BorrowAxisNow.createSubtask()

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(axisNowNotSupported, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Fulfillment can't proceed without a token.
   */

  @Test
  fun testAxisDownloadFailed() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(404)
    )

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(httpRequestFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Cancelling works.
   */

  @Test
  fun testAxisDownloadCancelled0() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()
    this.context.isCancelled = true

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskCancelled) {
      this.logger.error("exception: ", e)
    }

    /*
     * It is not the responsibility of subtasks to published "cancelled" states.
     */

    assertEquals(0, this.bookDatabaseEntry.entryWrites)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Cancelling works.
   */

  @Test
  fun testAxisDownloadCancelled1() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("Text!")
    )

    var eventCount = 0
    this.onEvent = { event ->
      ++eventCount
      if (eventCount >= 2) {
        this.context.isCancelled = true
      }
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskCancelled) {
      this.logger.error("exception: ", e)
    }

    /*
     * It is not the responsibility of subtasks to published "cancelled" states.
     */

    assertEquals(0, this.bookDatabaseEntry.entryWrites)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the server delivers something that isn't a fulfillment token, downloading fails.
   */

  @Test
  fun testAxisDownloadFailedWrongType() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("content-type", "text/plain")
        .setBody("What?")
    )

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(httpContentTypeIncompatible, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the server delivers something that isn't an fulfillment token, downloading fails.
   */

  @Test
  fun testAxisDownloadFailedUnparseable() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("content-type", axisNow.fullType)
        .setBody("Clearly not.")
    )

    this.axisNowService.onFulfill = { _, _ ->
      throw AxisNowFulfillmentException()
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(axisNowFulfillmentFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the AxisNowService fails, the download fails.
   */

  @Test
  fun testAxisNowServiceFails() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(this.validTokenResponse)

    this.axisNowService.onFulfill = { token, tempFactory ->
      throw AxisNowFulfillmentException()
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(axisNowFulfillmentFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license == null)
    assertTrue(this.axisHandle.info.userKey == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the AxisNowService library succeeds, the download succeeds.
   */

  @Test
  fun testAxisOk() {
    val task = BorrowAxisNow.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book").toUri()

    this.webServer.enqueue(this.validTokenResponse)

    this.axisNowService.onFulfill = { token, tempFactory ->
      val fakeBook = context.temporaryFile().apply { createNewFile() }
      val fakeLicense = context.temporaryFile().apply { createNewFile() }
      val fakeUserKey = context.temporaryFile().apply { createNewFile() }
      AxisNowFulfillment(fakeBook, fakeLicense, fakeUserKey)
    }

    try {
      task.execute(this.context)
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
      fail()
    }

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.axisHandle.info.license != null)
    assertTrue(this.axisHandle.info.userKey != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file != null)
  }
}
