package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
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
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.book_database.BookDRMInformationHandleACS
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseType
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
import org.nypl.simplified.books.formats.api.StandardFormatNames.adobeACSMFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericAudioBooks
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.opdsAcquisitionFeedEntry
import org.nypl.simplified.books.formats.api.StandardFormatNames.simplifiedBearerToken
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.TestDirectories.temporaryFileOf
import org.nypl.simplified.tests.books.audio.AudioBookSucceedingParsers.playerManifest
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.FeedRequirements
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.PathElement
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANABLE
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANED
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockAdobeAdeptConnector
import org.nypl.simplified.tests.mocking.MockAdobeAdeptExecutor
import org.nypl.simplified.tests.mocking.MockAdobeAdeptNetProvider
import org.nypl.simplified.tests.mocking.MockAdobeAdeptResourceProvider
import org.nypl.simplified.tests.mocking.MockAudioBookManifestStrategies
import org.nypl.simplified.tests.mocking.MockAxisNowService
import org.nypl.simplified.tests.mocking.MockBookFormatSupport
import org.nypl.simplified.tests.mocking.MockBorrowSubtaskDirectory
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockContentResolver
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BorrowTaskTest {

  private val validACSM = """
<fulfillmentToken
  xmlns="http://ns.adobe.com/adept"
  xmlns:f="http://purl.org/dc/elements/1.1/">
  <resourceItemInfo></resourceItemInfo>
  <metadata></metadata>
  <f:format>application/epub+zip</f:format>
</fulfillmentToken>
  """

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var adobeConnector: MockAdobeAdeptConnector
  private lateinit var adobeExecutor: MockAdobeAdeptExecutor
  private lateinit var adobeExecutorService: ExecutorService
  private lateinit var adobeNetProvider: MockAdobeAdeptNetProvider
  private lateinit var adobeResourceProvider: MockAdobeAdeptResourceProvider
  private lateinit var axisNowService: MockAxisNowService
  private lateinit var audioBookManifestStrategies: MockAudioBookManifestStrategies
  private lateinit var book: Book
  private lateinit var bookDatabase: BookDatabaseType
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
  private lateinit var profile: ProfileType
  private lateinit var profiles: ProfilesDatabaseType
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
        adobeExecutor = this.adobeExecutor,
        axisNowService = this.axisNowService,
        audioBookManifestStrategies = this.audioBookManifestStrategies,
        bookFormatSupport = this.bookFormatSupport,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        cacheDirectory = this.cacheDirectory,
        clock = { Instant.now() },
        contentResolver = this.contentResolver,
        httpClient = this.httpClient,
        profiles = this.profiles,
        services = this.services,
        subtasks = this.subtasks,
        temporaryDirectory = this.temporaryDirectory
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

  @BeforeEach
  fun testSetup() {
    this.webServer = MockWebServer()
    this.webServer.start(20000)

    this.opdsEmptyFeedEntry =
      BorrowTestFeeds.opdsEmptyFeedEntryOfType()

    this.opdsOpenEPUBFeedEntry =
      BorrowTestFeeds.opdsOpenAccessFeedEntryOfType(
        this.webServer,
        genericEPUBFiles.fullType
      )

    this.accountId =
      AccountID.generate()
    this.bookID =
      BookIDs.newFromOPDSEntry(this.opdsOpenEPUBFeedEntry)

    val androidContext =
      Mockito.mock(Context::class.java)

    this.bookDatabase =
      BookDatabase.open(
        context = androidContext,
        parser = OPDSJSONParser.newParser(),
        serializer = OPDSJSONSerializer.newSerializer(),
        owner = this.accountId,
        directory = TestDirectories.temporaryDirectory()
      )

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
      Mockito.mock(ProfileType::class.java)
    this.profiles =
      Mockito.mock(ProfilesDatabaseType::class.java)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")
    this.services =
      MutableServiceDirectory()
    this.subtasks =
      MockBorrowSubtaskDirectory()

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

    this.subtasks.subtasks =
      BorrowSubtasks.directory()
        .subtasks

    val profileId = ProfileID.generate()

    Mockito.`when`(this.profiles.profiles())
      .thenReturn(sortedMapOf(Pair(profileId, this.profile)))
    Mockito.`when`(this.profile.id)
      .thenReturn(profileId)
    Mockito.`when`(this.profile.account(this.accountId))
      .thenReturn(this.account)
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)
    Mockito.`when`(this.account.provider)
      .thenReturn(this.accountProvider)

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
    this.axisNowService =
      MockAxisNowService()
  }

  @AfterEach
  fun tearDown() {
    this.bookRegistrySub?.dispose()
    this.webServer.close()
  }

  private fun recordBookEvent(event: BookStatusEvent) {
    this.logger.debug("event: {}", event)
    val status = event.statusNow!!
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
      BorrowRequest.Start(this.accountId, this.profile.id, this.opdsEmptyFeedEntry)
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
      BorrowRequest.Start(this.accountId, this.profile.id, this.opdsEmptyFeedEntry)
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
      BorrowRequest.Start(this.accountId, this.profile.id, this.opdsEmptyFeedEntry)
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
      BorrowRequest.Start(this.accountId, this.profile.id, this.opdsOpenEPUBFeedEntry)
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
    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("A cold star looked down on his creations")
    )

    val request =
      BorrowRequest.Start(this.accountId, this.profile.id, this.opdsOpenEPUBFeedEntry)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)
    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry = this.bookDatabase.entry(this.bookID)
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)!!

    assertEquals("A cold star looked down on his creations", handle.format.file!!.readText())
  }

  /**
   * Creating a loan and then downloading an EPUB succeeds.
   */

  @Test
  fun testLoanEPUB() {
    val loanableRequirements =
      FeedRequirements(
        status = LOANABLE,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(opdsAcquisitionFeedEntry.fullType, "/loan"),
          PathElement(genericEPUBFiles.fullType, "/epub")
        )
      )

    val loanedRequirements =
      FeedRequirements(
        status = LOANED,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(genericEPUBFiles.fullType, "/epub")
        )
      )

    val loanable =
      BorrowTestFeeds.feed(loanableRequirements)
    val loaned =
      BorrowTestFeeds.feedText(loanedRequirements)

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
      BorrowRequest.Start(this.accountId, this.profile.id, loanable)
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

    val entry = this.bookDatabase.entry(this.bookID)
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)!!

    assertEquals("A cold star looked down on his creations", handle.format.file!!.readText())
  }

  /**
   * Creating a loan and then downloading an EPUB via an ACSM file succeeds.
   */

  @Test
  fun testLoanACSM() {
    val loanableRequirements =
      FeedRequirements(
        status = LOANABLE,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(opdsAcquisitionFeedEntry.fullType, "/loan"),
          PathElement(adobeACSMFiles.fullType, "/acsm"),
          PathElement(genericEPUBFiles.fullType, "/epub")
        )
      )

    val loanedRequirements =
      FeedRequirements(
        status = LOANED,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(adobeACSMFiles.fullType, "/acsm"),
          PathElement(genericEPUBFiles.fullType, "/epub")
        )
      )

    val loanable =
      BorrowTestFeeds.feed(loanableRequirements)
    val loaned =
      BorrowTestFeeds.feedText(loanedRequirements)

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry.fullType)
        .setBody(loaned)
    )

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", adobeACSMFiles.fullType)
        .setBody(this.validACSM)
    )

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

    val request =
      BorrowRequest.Start(this.accountId, this.profile.id, loanable)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry =
      this.bookDatabase.entry(this.bookID)
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)!!
    val drm =
      handle.drmInformationHandle as BookDRMInformationHandleACS

    assertEquals("A cold star looked down on his creations", handle.format.file!!.readText())
    assertEquals(adobeLoanID, drm.info.rights!!.second.id)
    assertNotNull(drm.info.acsmFile)
  }

  /**
   * Creating a loan and then downloading an audio book succeeds.
   */

  @Test
  fun testLoanAudioBook() {
    val loanableRequirements =
      FeedRequirements(
        status = LOANABLE,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(opdsAcquisitionFeedEntry.fullType, "/loan"),
          PathElement(genericAudioBooks.first().fullType, "/audio-book")
        )
      )

    val loanedRequirements =
      FeedRequirements(
        status = LOANED,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(genericAudioBooks.first().fullType, "/audio-book")
        )
      )

    val loanable =
      BorrowTestFeeds.feed(loanableRequirements)
    val loaned =
      BorrowTestFeeds.feedText(loanedRequirements)

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

    this.audioBookManifestStrategies.strategy.onExecute = {
      val taskRecorder = TaskRecorder.create()
      taskRecorder.beginNewStep("Succeeding...")
      taskRecorder.finishSuccess(
        AudioBookManifestData(
          manifest = playerManifest,
          fulfilled = ManifestFulfilled(
            contentType = genericAudioBooks.first(),
            data = playerManifest.originalBytes
          )
        )
      )
    }

    val request =
      BorrowRequest.Start(this.accountId, this.profile.id, loanable)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry = this.bookDatabase.entry(this.bookID)
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)!!

    val manifest = handle.format.manifest!!
    assertEquals(this.webServer.url("/audio-book").toUri(), manifest.manifestURI)
    assertArrayEquals(playerManifest.originalBytes, manifest.manifestFile.readBytes())
  }

  /**
   * Borrowing via a bearer token works.
   */

  @Test
  fun testBearerTokenEPUB() {
    val loanedRequirements =
      FeedRequirements(
        status = LOANED,
        base = this.webServer.url("/").toUri(),
        path = listOf(
          PathElement(simplifiedBearerToken.fullType, "/epub"),
          PathElement(genericEPUBFiles.fullType, "/epub")
        )
      )

    val loanable =
      BorrowTestFeeds.feed(loanedRequirements)

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", simplifiedBearerToken.fullType)
        .setBody(
          """{
          "access_token": "abcd",
          "expires_in": 1000,
          "location": "http://localhost:20000/book.epub"
        }
          """.trimIndent()
        )
    )

    this.webServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("A cold star looked down on his creations")
    )

    val request =
      BorrowRequest.Start(this.accountId, this.profile.id, loanable)
    val task =
      this.createTask(request)

    val result = this.executeAssumingSuccess(task)

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val entry = this.bookDatabase.entry(this.bookID)
    val handle =
      entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)!!

    assertEquals("A cold star looked down on his creations", handle.format.file!!.readText())
  }
}
