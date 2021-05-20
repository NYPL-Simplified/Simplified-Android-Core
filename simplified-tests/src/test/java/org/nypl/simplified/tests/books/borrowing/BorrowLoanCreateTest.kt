package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import io.reactivex.disposables.Disposable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.DateTime
import org.joda.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.bearer_token.LSHTTPBearerTokenInterceptors
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
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatus.FailedLoan
import org.nypl.simplified.books.book_registry.BookStatus.Held.HeldInQueue
import org.nypl.simplified.books.book_registry.BookStatus.Held.HeldReady
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedNotDownloaded
import org.nypl.simplified.books.book_registry.BookStatus.RequestingLoan
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpConnectionFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpContentTypeIncompatible
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpRequestFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryHoldable
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryParseError
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.requiredURIMissing
import org.nypl.simplified.books.borrowing.internal.BorrowLoanCreate
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskHaltedEarly
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.opdsAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockBookDatabase
import org.nypl.simplified.tests.mocking.MockBookDatabaseEntry
import org.nypl.simplified.tests.mocking.MockBorrowContext
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockContentResolver
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class BorrowLoanCreateTest {

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var bookDatabase: MockBookDatabase
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

  private val logger = LoggerFactory.getLogger(BorrowLoanCreateTest::class.java)

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
    this.bookID =
      BookIDs.newFromText("x")
    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")

    Mockito.`when`(this.account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("someone"),
            password = AccountPassword("not a password"),
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

    val bookInitial =
      Book(
        id = this.bookID,
        account = this.accountId,
        cover = null,
        thumbnail = null,
        entry = OPDSAcquisitionFeedEntry.newBuilder("x", "Title", DateTime.now(), OPDSAvailabilityLoanable.get()).build(),
        formats = listOf()
      )

    this.bookDatabase =
      MockBookDatabase(this.accountId)
    this.bookDatabaseEntry =
      MockBookDatabaseEntry(bookInitial)

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

    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)

    this.webServer = MockWebServer()
    this.webServer.start(20000)
  }

  private fun recordBookEvent(event: BookStatusEvent) {
    this.logger.debug("event: {}", event)
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
   * A loan can't be performed if no URI is available.
   */

  @Test
  fun testNoURI() {
    val task = BorrowLoanCreate.createSubtask()

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
    assertEquals(requiredURIMissing, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * A failing HTTP connection fails the loan.
   */

  @Test
  fun testHTTPConnectionFails() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
    assertEquals(httpConnectionFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * A 404 fails the loan.
   */

  @Test
  fun testHTTP404Fails() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()

    this.webServer.enqueue(MockResponse().setResponseCode(404))

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
    assertEquals(httpRequestFailed, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * An incompatible MIME type fails the loan.
   */

  @Test
  fun testMIMEIncompatibleFails() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/plain")

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
    assertEquals(httpContentTypeIncompatible, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * An unparseable OPDS feed entry fails the loan.
   */

  @Test
  fun testLoanUnparseableOPDS() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody("Charlemagne used to always call me Durandana, the fruitcake.")

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    this.verifyBookRegistryHasStatus(FailedLoan::class.java)
    assertEquals(opdsFeedEntryParseError, this.taskRecorder.finishFailure<Unit>().lastErrorCode)
    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * A loan is created.
   */

  @Test
  fun testLoanOkEPUB() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val feedText = """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link href="${this.webServer.url("/next")}" rel="http://opds-spec.org/acquisition" type="application/epub+zip">
    <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
    <opds:holds total="0" />
    <opds:copies available="5" total="5" />
  </link>
</entry>
    """.trimIndent()

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody(feedText)

    this.webServer.enqueue(response)

    task.execute(this.context)

    assertEquals(this.webServer.url("/next").toUri(), this.context.receivedURIs[0])
    assertEquals(1, this.context.receivedURIs.size)

    this.verifyBookRegistryHasStatus(LoanedNotDownloaded::class.java)
    assertEquals(1, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)

    val request0 = this.webServer.takeRequest()
    assertEquals("Basic c29tZW9uZTpub3QgYSBwYXNzd29yZA==", request0.getHeader("Authorization"))
  }

  /**
   * A file is downloaded even if it has to go through a bearer token.
   */

  @Test
  fun testLoanOkEPUBBearerToken() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val feedText = """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link href="${this.webServer.url("/next")}" rel="http://opds-spec.org/acquisition" type="application/epub+zip">
    <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
    <opds:holds total="0" />
    <opds:copies available="5" total="5" />
  </link>
</entry>
    """.trimIndent()

    val response0 =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", LSHTTPBearerTokenInterceptors.bearerTokenContentType)
        .setBody(
          """{
          "access_token": "abcd",
          "expires_in": 1000,
          "location": "${this.webServer.url("/book.epub")}"
        }
          """.trimIndent()
        )

    val response1 =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody(feedText)

    this.webServer.enqueue(response0)
    this.webServer.enqueue(response1)

    task.execute(this.context)

    val sent0 = this.webServer.takeRequest()
    assertEquals("Basic c29tZW9uZTpub3QgYSBwYXNzd29yZA==", sent0.getHeader("Authorization"))
    val sent1 = this.webServer.takeRequest()
    assertEquals("Bearer abcd", sent1.getHeader("Authorization"))

    this.verifyBookRegistryHasStatus(LoanedNotDownloaded::class.java)
    assertEquals(1, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * If the loan is held, the book becomes held.
   */

  @Test
  fun testLoanHeld() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val feedText = """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link rel="http://opds-spec.org/acquisition" type="application/epub+zip">
    <opds:availability since="2020-09-17T16:48:51+0000" status="reserved" until="2020-09-17T16:48:51+0000"/>
    <opds:holds total="0"/>
    <opds:copies available="0" total="5"/>
  </link>
</entry>
    """.trimIndent()

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody(feedText)

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: BorrowSubtaskHaltedEarly) {
      this.logger.debug("halted early: ", e)
    } catch (e: Exception) {
      this.logger.debug("error: ", e)
      throw IllegalStateException(e)
    }

    this.verifyBookRegistryHasStatus(HeldInQueue::class.java)
    assertEquals(1, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(HeldInQueue::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * If the loan is held, the book becomes held/ready.
   */

  @Test
  fun testLoanHeldReady() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val feedText = """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link rel="http://opds-spec.org/acquisition" type="application/epub+zip">
    <opds:availability since="2020-09-17T16:48:51+0000" status="ready" until="2020-09-17T16:48:51+0000"/>
    <opds:holds total="0"/>
    <opds:copies available="0" total="5"/>
  </link>
</entry>
    """.trimIndent()

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody(feedText)

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: BorrowSubtaskHaltedEarly) {
      this.logger.debug("halted early: ", e)
    } catch (e: Exception) {
      this.logger.debug("error: ", e)
      throw IllegalStateException(e)
    }

    this.verifyBookRegistryHasStatus(HeldReady::class.java)
    assertEquals(1, this.bookDatabaseEntry.entryWrites)
    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(HeldReady::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * If the loan is holdable, the book becomes holdable.
   */

  @Test
  fun testLoanHoldable() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val feedText = """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link rel="http://opds-spec.org/acquisition" type="application/epub+zip">
    <opds:holds total="0"/>
    <opds:copies available="0" total="5"/>
  </link>
</entry>
    """.trimIndent()

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", opdsAcquisitionFeedEntry)
        .setBody(feedText)

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assertions.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(
      opdsFeedEntryHoldable,
      this.taskRecorder.finishFailure<Unit>().lastErrorCode
    )

    assertEquals(1, this.bookDatabaseEntry.entryWrites)
    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(FailedLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }

  /**
   * A loan can't be created twice.
   */

  @Test
  fun testLoanAlreadyExists() {
    val task = BorrowLoanCreate.createSubtask()

    this.context.currentURIField =
      this.webServer.url("/book.epub").toUri()
    this.context.currentAcquisitionPathElement =
      OPDSAcquisitionPathElement(opdsAcquisitionFeedEntry, null)
    this.context.currentRemainingOPDSPathElements =
      listOf(OPDSAcquisitionPathElement(genericEPUBFiles, null))

    val response =
      MockResponse()
        .setResponseCode(400)
        .setHeader("Content-Type", "application/api-problem+json")
        .setBody(
          """{
  "type": "http://librarysimplified.org/terms/problem/loan-already-exists"
}
          """.trimIndent()
        )

    this.webServer.enqueue(response)

    task.execute(this.context)

    assertEquals(0, this.bookDatabaseEntry.entryWrites)

    assertEquals(RequestingLoan::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(LoanedNotDownloaded::class.java, this.bookStates.removeAt(0).javaClass)
    assertEquals(0, this.bookStates.size)
  }
}
