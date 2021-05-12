package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import org.joda.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatus.Downloading
import org.nypl.simplified.books.book_registry.BookStatus.FailedDownload
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedDownloaded
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.borrowing.internal.BorrowAudioBook
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.audioStrategyFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.requiredURIMissing
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericAudioBooks
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockAudioBookManifestStrategies
import org.nypl.simplified.tests.mocking.MockBookDatabase
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntry
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.tests.mocking.MockBorrowContext
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockContentResolver
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class BorrowAudioBookTest {

  private lateinit var audioBookHandle: MockBookDatabaseEntryFormatHandleAudioBook
  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var audioBookManifestStrategies: MockAudioBookManifestStrategies
  private lateinit var bookDatabase: BookDatabaseType
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
  private lateinit var manifestData: AudioBookManifestData
  private lateinit var profile: ProfileReadableType
  private lateinit var services: MutableServiceDirectory
  private lateinit var taskRecorder: TaskRecorderType
  private var bookRegistrySub: Disposable? = null

  private val logger = LoggerFactory.getLogger(BorrowAudioBookTest::class.java)

  private fun verifyBookRegistryHasStatus(clazz: Class<*>) {
    val registryStatus = this.bookRegistry.bookStatusOrNull(this.bookID)!!
    assertEquals(clazz, registryStatus.javaClass)
  }

  @BeforeEach
  fun testSetup() {
    this.taskRecorder =
      TaskRecorder.create()
    this.contentResolver =
      MockContentResolver()
    this.bundledContent =
      MockBundledContentResolver()
    this.services =
      MutableServiceDirectory()

    this.audioBookManifestStrategies =
      MockAudioBookManifestStrategies()

    this.bookFormatSupport =
      Mockito.mock(BookFormatSupportType::class.java)
    this.bookRegistry =
      BookRegistry.create()
    this.bookStates =
      mutableListOf()
    this.bookEvents =
      mutableListOf()
    this.bookRegistrySub =
      this.bookRegistry.bookEvents()
        .subscribe(this::recordBookEvent)

    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")

    Mockito.`when`(this.account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
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
            applicationName = "simplified-tests",
            applicationVersion = "999.999.0",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

    this.accountId =
      AccountID.generate()

    val initialFeedEntry =
      BorrowTestFeeds.opdsContentURILoanedFeedEntryOfType(genericAudioBooks.first().fullType)
    this.bookID =
      BookIDs.newFromOPDSEntry(initialFeedEntry)

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
    this.audioBookHandle =
      MockBookDatabaseEntryFormatHandleAudioBook(this.bookID)
    this.bookDatabaseEntry.formatHandlesField.clear()
    this.bookDatabaseEntry.formatHandlesField.add(this.audioBookHandle)

    this.context =
      MockBorrowContext(
        account = this.account,
        bookDatabaseEntry = this.bookDatabaseEntry,
        bookInitial = bookInitial,
        bookRegistry = this.bookRegistry,
        bundledContent = this.bundledContent,
        clock = { Instant.now() },
        contentResolver = this.contentResolver,
        httpClient = this.httpClient,
        isCancelled = false,
        logger = this.logger,
        taskRecorder = this.taskRecorder,
        temporaryDirectory = TestDirectories.temporaryDirectory()
      )

    this.context.audioBookManifestStrategies =
      this.audioBookManifestStrategies
    this.context.services =
      this.services

    this.manifestData = this.fakeManifestData()
  }

  private fun fakeManifestData(): AudioBookManifestData {
    val data =
      BorrowAudioBookTest::class.java.getResourceAsStream(
        "/org/nypl/simplified/tests/books/basic-manifest.json"
      )!!
        .readBytes()
    val manifestResult =
      ManifestParsers.parse(URI.create("urn:basic-manifest.json"), data)
        as ParseResult.Success
    val manifest =
      manifestResult.result

    return AudioBookManifestData(
      manifest = manifest,
      fulfilled = ManifestFulfilled(genericAudioBooks.first(), data)
    )
  }

  private fun recordBookEvent(event: BookStatusEvent) {
    this.logger.debug("event: {}", event)
    val status = event.statusNow!!
    this.logger.debug("status: {}", status)
    this.bookStates.add(status)
    this.bookEvents.add(event)
  }

  /**
   * A copy can't be performed if no URI is available.
   */

  @Test
  fun testNoURI() {
    val task = BorrowAudioBook.createSubtask()

    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(genericAudioBooks.first(), null)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(requiredURIMissing, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.audioBookHandle.format.manifest == null)
  }

  /**
   * If the audio book strategy fails, the download fails.
   */

  @Test
  fun testStrategyFails() {
    val task = BorrowAudioBook.createSubtask()

    this.context.currentURIField =
      URI.create("urn:whatever")
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(genericAudioBooks.first(), null)

    this.audioBookManifestStrategies.strategy.onExecute = {
      TaskResult.fail("Running", "Water system breakdown at sector 6", audioStrategyFailed)
    }

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(audioStrategyFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedDownload::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.audioBookHandle.format.manifest == null)
  }

  /**
   * If the audio book strategy succeeds, the download succeeds.
   */

  @Test
  fun testStrategySucceeds() {
    val task = BorrowAudioBook.createSubtask()

    this.context.currentURIField =
      URI.create("urn:whatever")
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(genericAudioBooks.first(), null)

    this.audioBookManifestStrategies.strategy.onExecute = {
      val recorder = TaskRecorder.create()
      recorder.beginNewStep("Trying strategy!")
      recorder.currentStepSucceeded("Went fine.")
      recorder.finishSuccess(this.manifestData)
    }

    task.execute(this.context)

    this.verifyBookRegistryHasStatus(LoanedDownloaded::class.java)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(Downloading::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    assertTrue(this.audioBookHandle.format.manifest != null)
  }
}
