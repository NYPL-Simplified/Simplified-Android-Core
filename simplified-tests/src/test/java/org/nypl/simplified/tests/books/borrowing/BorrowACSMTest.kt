package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import one.irradia.mime.api.MIMEType
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
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
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
import org.nypl.simplified.books.borrowing.BorrowTimeoutConfiguration
import org.nypl.simplified.books.borrowing.internal.BorrowACSM
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.accountCredentialsRequired
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNoCredentialsPost
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNoCredentialsPre
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNotSupported
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsTimedOut
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsUnparseableACSM
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpContentTypeIncompatible
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpRequestFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskHaltedEarly
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames.adobeACSMFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.TestDirectories.temporaryFileOf
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockAdobeAdeptConnector
import org.nypl.simplified.tests.mocking.MockAdobeAdeptExecutor
import org.nypl.simplified.tests.mocking.MockAdobeAdeptNetProvider
import org.nypl.simplified.tests.mocking.MockAdobeAdeptResourceProvider
import org.nypl.simplified.tests.mocking.MockBookDatabase
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntry
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.tests.mocking.MockBorrowContext
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockContentResolver
import org.nypl.simplified.tests.mocking.MockDRMInformationACSHandle
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BorrowACSMTest {

  private val validACSM = """
<fulfillmentToken
  xmlns="http://ns.adobe.com/adept"
  xmlns:f="http://purl.org/dc/elements/1.1/">
  <resourceItemInfo></resourceItemInfo>
  <metadata></metadata>
  <f:format>application/epub+zip</f:format>
</fulfillmentToken>
  """.trimIndent()

  private val validACSMResponse =
    MockResponse()
      .setResponseCode(200)
      .setHeader("content-type", adobeACSMFiles.fullType)
      .setBody(this.validACSM)

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var acsHandle: MockDRMInformationACSHandle
  private lateinit var adobeConnector: MockAdobeAdeptConnector
  private lateinit var adobeExecutor: MockAdobeAdeptExecutor
  private lateinit var adobeExecutorService: ExecutorService
  private lateinit var adobeNetProvider: MockAdobeAdeptNetProvider
  private lateinit var adobeResourceProvider: MockAdobeAdeptResourceProvider
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

  private val logger = LoggerFactory.getLogger(BorrowACSMTest::class.java)

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
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("vendor"),
              clientToken = AccountAuthenticationAdobeClientToken(
                userName = "user",
                password = "password",
                rawToken = "b85e7fd7-cf6e-4e39-8da6-8df8c9ee9779"
              ),
              deviceManagerURI = URI.create("http://www.example.com"),
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                deviceID = AdobeDeviceID("ca887d21-a56c-4314-811e-952d885d2115"),
                userID = AdobeUserID("19b25c06-8b39-4643-8813-5980bee45651")
              )
            ),
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
            applicationName = "simplified-tests",
            applicationVersion = "999.999.0",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
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
    this.acsHandle =
      MockDRMInformationACSHandle()
    this.bookDatabaseEPUBHandle.drmInformationHandleField =
      this.acsHandle

    this.adobeNetProvider =
      MockAdobeAdeptNetProvider()
    this.adobeResourceProvider =
      MockAdobeAdeptResourceProvider()
    this.adobeConnector =
      MockAdobeAdeptConnector(this.adobeNetProvider, this.adobeResourceProvider)
    this.adobeExecutorService =
      Executors.newSingleThreadExecutor()
    this.adobeExecutor =
      MockAdobeAdeptExecutor(this.adobeExecutorService, this.adobeConnector)

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

    this.context.adobeExecutor = this.adobeExecutor
    this.context.opdsAcquisitionPath =
      OPDSAcquisitionPath(
        OPDSAcquisition(
          OPDSAcquisition.Relation.ACQUISITION_GENERIC,
          this.webServer.url("/book.acsm").toUri(),
          adobeACSMFiles,
          listOf()
        ),
        listOf(
          OPDSAcquisitionPathElement(
            adobeACSMFiles,
            this.webServer.url("/book.acsm").toUri()
          ),
          OPDSAcquisitionPathElement(
            genericEPUBFiles,
            this.webServer.url("/book.epub").toUri()
          )
        )
      )

    this.context.currentRemainingOPDSPathElements =
      listOf(
        OPDSAcquisitionPathElement(
          genericEPUBFiles,
          this.webServer.url("/book.epub").toUri()
        )
      )

    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(adobeACSMFiles, null)
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
    this.adobeExecutorService.shutdown()
  }

  /**
   * If the application manages to get as far as this subtask without ACS support, then
   * the subtask fails with an appropriate "not supported" error code.
   */

  @Test
  fun testNotSupported() {
    this.context.adobeExecutor = null

    val task = BorrowACSM.createSubtask()

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(acsNotSupported, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Fulfillment can't proceed without the required credentials.
   */

  @Test
  fun testMissingCredentials0() {
    val task = BorrowACSM.createSubtask()

    Mockito.`when`(this.account.loginState)
      .thenReturn(AccountNotLoggedIn)

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(accountCredentialsRequired, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Fulfillment can't proceed without the required credentials.
   */

  @Test
  fun testMissingCredentials1() {
    val task = BorrowACSM.createSubtask()

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

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(acsNoCredentialsPre, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Fulfillment can't proceed without the required credentials.
   */

  @Test
  fun testMissingCredentials2() {
    val task = BorrowACSM.createSubtask()

    Mockito.`when`(this.account.loginState)
      .thenReturn(
        AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              AdobeVendorID("vendor"),
              AccountAuthenticationAdobeClientToken("user", "password", "b85e7fd7-cf6e-4e39-8da6-8df8c9ee9779"),
              null,
              null
            ),
            authenticationDescription = "Basic",
            annotationsURI = URI("https://www.example.com")
          )
        )
      )

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(acsNoCredentialsPost, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Fulfillment can't proceed without an ACSM file.
   */

  @Test
  fun testACSMDownloadFailed() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

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

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Cancelling works.
   */

  @Test
  fun testACSMDownloadCancelled0() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()
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

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * Cancelling works.
   */

  @Test
  fun testACSMDownloadCancelled1() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

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

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the server delivers something that isn't an ACSM file, downloading fails.
   */

  @Test
  fun testACSMDownloadFailedWrongType() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

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

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the server delivers something that isn't an ACSM file, downloading fails.
   */

  @Test
  fun testACSMDownloadFailedUnparseable() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("content-type", adobeACSMFiles.fullType)
        .setBody("Clearly not.")
    )

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(acsUnparseableACSM, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If the ACSM downloads successfully, but the target content type is nonsense, downloading
   * fails.
   */

  @Test
  fun testACSMNonsenseFormat() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()
    this.context.opdsAcquisitionPath =
      this.context.opdsAcquisitionPath.copy(
        elements = listOf(
          OPDSAcquisitionPathElement(
            adobeACSMFiles,
            this.webServer.url("/book.acsm").toUri()
          ),
          OPDSAcquisitionPathElement(
            MIMEType("text", "plain", mapOf()),
            this.webServer.url("/book.epub").toUri()
          )
        )
      )
    this.context.currentRemainingOPDSPathElements =
      listOf(
        OPDSAcquisitionPathElement(
          MIMEType("text", "plain", mapOf()),
          this.webServer.url("/book.epub").toUri()
        )
      )

    this.webServer.enqueue(this.validACSMResponse)

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.debug("correctly failed: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals("noFormatHandle", this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile == null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If ACS library fails, the download fails.
   */

  @Test
  fun testACSFails0() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

    this.webServer.enqueue(this.validACSMResponse)

    this.adobeConnector.onFulfill = { listener, acsm, user ->
      listener.onFulfillmentFailure("E_DEFECTIVE")
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals("ACS: E_DEFECTIVE", this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If ACS library times out, the download fails.
   */

  @Test
  fun testACSFails1() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()
    this.context.adobeExecutorTimeout =
      BorrowTimeoutConfiguration(10L, TimeUnit.MILLISECONDS)

    this.webServer.enqueue(this.validACSMResponse)

    /*
     * Wait indefinitely inside the connector so that we time out waiting for it.
     */

    this.adobeConnector.onFulfill = { listener, acsm, user ->
      Thread.sleep(10_000L)
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals(acsTimedOut, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If ACS library fails, the download fails.
   */

  @Test
  fun testACSFails2() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

    this.webServer.enqueue(this.validACSMResponse)

    this.adobeConnector.onFulfill = { listener, acsm, user ->
      throw IllegalStateException("OH DEAR!")
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskFailed) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedDownload::class.java)
    assertEquals("ACS: class java.lang.IllegalStateException", this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file == null)
  }

  /**
   * If ACS library succeeds, the download succeeds.
   */

  @Test
  fun testACSOk() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()

    this.webServer.enqueue(this.validACSMResponse)

    val temporaryFile =
      temporaryFileOf("book.epub", "A cold star looked down on his creations")
    val adobeLoanID =
      AdobeLoanID("4cca8916-d0fe-44ed-85d9-a8212764375d")

    this.adobeConnector.onFulfill = { listener, acsm, user ->
      listener.onFulfillmentSuccess(
        temporaryFile,
        AdobeAdeptLoan(
          adobeLoanID,
          "You're a blank. You don't have rights.".toByteArray(),
          false
        )
      )
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskHaltedEarly) {
      this.logger.debug("correctly halted early: ", e)
    }

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file != null)
  }

  /**
   * Obnoxiously fast updates are throttled.
   */

  @Test
  fun testACSOkThrottled() {
    val task = BorrowACSM.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.acsm").toUri()
    this.context.adobeExecutorTimeout =
      BorrowTimeoutConfiguration(10L, TimeUnit.SECONDS)

    this.webServer.enqueue(this.validACSMResponse)

    val temporaryFile =
      temporaryFileOf("book.epub", "A cold star looked down on his creations")
    val adobeLoanID =
      AdobeLoanID("4cca8916-d0fe-44ed-85d9-a8212764375d")

    this.adobeConnector.onFulfill = { listener, acsm, user ->
      for (i in 0 until 3000) {
        listener.onFulfillmentProgress(i.toDouble() / 3000.0)
        Thread.sleep(1L)
      }

      listener.onFulfillmentSuccess(
        temporaryFile,
        AdobeAdeptLoan(
          adobeLoanID,
          "You're a blank. You don't have rights.".toByteArray(),
          false
        )
      )
    }

    try {
      task.execute(this.context)
      fail()
    } catch (e: BorrowSubtaskHaltedEarly) {
      this.logger.debug("correctly halted early: ", e)
    }

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    var downloadCount = 0
    while (true) {
      if (this.bookStates.get(0) is Downloading) {
        ++downloadCount
        this.bookStates.removeAt(0)
      } else {
        break
      }
    }

    assertTrue(downloadCount >= 3)
    assertTrue(downloadCount < 10)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.acsHandle.info.acsmFile != null)
    assertTrue(this.bookDatabaseEPUBHandle.format.file != null)
  }
}
