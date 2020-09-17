package org.nypl.simplified.tests.books.borrowing

import android.content.Context
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import one.irradia.mime.api.MIMEType
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.bearer_token.LSHTTPBearerTokenInterceptors
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowDirectDownload
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.requiredURIMissing
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericPDFFiles
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.TestDirectories
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

class BorrowDirectDownloadTest {

  private lateinit var epubHandle: BookDatabaseEntryFormatHandleEPUB
  private lateinit var pdfHandle: BookDatabaseEntryFormatHandlePDF
  private lateinit var webServer: MockWebServer
  private lateinit var taskRecorder: TaskRecorderType
  private lateinit var context: BorrowContextType
  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var bookDatabase: BookDatabaseType
  private lateinit var bookDatabaseEntry: BookDatabaseEntryType
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookID: BookID
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var httpClient: LSHTTPClientType
  private lateinit var profile: ProfileReadableType

  private val logger = LoggerFactory.getLogger(BorrowDirectDownloadTest::class.java)

  private fun verifyBookRegistryHasFailureStatus() {
    val registryStatus = this.bookRegistry.bookStatusOrNull(this.bookID)!!
    assertEquals(BookStatus.FailedLoan::class.java, registryStatus.javaClass)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  @Before
  fun testSetup() {
    this.context =
      Mockito.mock(BorrowContextType::class.java)
    this.taskRecorder =
      TaskRecorder.create()

    this.bookFormatSupport =
      Mockito.mock(BookFormatSupportType::class.java)
    this.bookRegistry =
      BookRegistry.create()
    this.bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    this.bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    this.pdfHandle =
      Mockito.mock(BookDatabaseEntryFormatHandlePDF::class.java)
    this.epubHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    this.accountId =
      AccountID.generate()
    this.bookID =
      BookIDs.newFromText("x")
    this.account =
      Mockito.mock(AccountType::class.java)
    this.accountProvider =
      MockAccountProviders.fakeProvider("urn:uuid:ea9480d4-5479-4ef1-b1d1-84ccbedb680f")

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

    Mockito.`when`(this.context.clock)
      .thenReturn({ Instant.now() })
    Mockito.`when`(this.context.bookDatabaseEntry)
      .thenReturn(this.bookDatabaseEntry)
    Mockito.`when`(this.context.taskRecorder)
      .thenReturn(this.taskRecorder)
    Mockito.`when`(this.context.httpClient)
      .thenReturn(this.httpClient)
    Mockito.`when`(this.context.temporaryFile())
      .then {
        this.logger.debug("creating temporary directory")
        return@then File(TestDirectories.temporaryDirectory(), "${UUID.randomUUID()}.tmp")
      }

    this.webServer = MockWebServer()
    this.webServer.start(20000)
  }

  @After
  fun tearDown() {
    this.webServer.close()
  }

  /**
   * A direct download can't be performed if no URI is available.
   */

  @Test
  fun testNoURI() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.context.currentURICheck())
      .then {
        this.taskRecorder.currentStepFailed("Missing URI", requiredURIMissing)
        throw BorrowSubtaskException.BorrowSubtaskFailed()
      }

    try {
      task.execute(this.context)
      Assert.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(
      requiredURIMissing,
      this.taskRecorder.finishFailure<Unit>().lastErrorCode
    )
  }

  /**
   * A failing HTTP connection fails the download.
   */

  @Test
  fun testHTTPConnectionFails() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())

    try {
      task.execute(this.context)
      Assert.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(
      BorrowErrorCodes.httpConnectionFailed,
      this.taskRecorder.finishFailure<Unit>().lastErrorCode
    )
  }

  /**
   * A 404 fails the download.
   */

  @Test
  fun testHTTP404Fails() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())

    this.webServer.enqueue(MockResponse().setResponseCode(404))

    try {
      task.execute(this.context)
      Assert.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(
      BorrowErrorCodes.httpRequestFailed,
      this.taskRecorder.finishFailure<Unit>().lastErrorCode
    )
  }

  /**
   * An incompatible MIME type fails the download.
   */

  @Test
  fun testMIMEIncompatibleFails() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())

    Mockito.`when`(this.context.currentAcquisitionPathElement)
      .thenReturn(OPDSAcquisitionPathElement(
        MIMEType("application", "pdf", mapOf()),
        null
      ))

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/plain")

    this.webServer.enqueue(response)

    try {
      task.execute(this.context)
      Assert.fail()
    } catch (e: Exception) {
      this.logger.error("exception: ", e)
    }

    assertEquals(
      BorrowErrorCodes.httpContentTypeIncompatible,
      this.taskRecorder.finishFailure<Unit>().lastErrorCode
    )
  }

  /**
   * A file is downloaded.
   */

  @Test
  fun testDownloadOkPDF() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.bookDatabaseEntry.findFormatHandleForContentType(genericPDFFiles))
      .thenReturn(this.pdfHandle)
    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentAcquisitionPathElement)
      .thenReturn(OPDSAcquisitionPathElement(
        genericPDFFiles,
        null
      ))

    var savedData = ""
    Mockito.`when`(this.pdfHandle.copyInBook(this.anyNonNull()))
      .then {
        savedData = (it.getArgument(0) as File).readText()
        Unit
      }

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/pdf")
        .setBody("PDF!")

    this.webServer.enqueue(response)

    task.execute(this.context)

    assertEquals("PDF!", savedData)
  }

  /**
   * A file is downloaded.
   */

  @Test
  fun testDownloadOkEPUB() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.bookDatabaseEntry.findFormatHandleForContentType(genericEPUBFiles))
      .thenReturn(this.epubHandle)
    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentAcquisitionPathElement)
      .thenReturn(OPDSAcquisitionPathElement(
        genericEPUBFiles,
        null
      ))

    var savedData = ""
    Mockito.`when`(this.epubHandle.copyInBook(this.anyNonNull()))
      .then {
        savedData = (it.getArgument(0) as File).readText()
        Unit
      }

    val response =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/epub+zip")
        .setBody("EPUB!")

    this.webServer.enqueue(response)

    task.execute(this.context)

    assertEquals("EPUB!", savedData)
  }

  /**
   * A file is downloaded even if it has to go through a bearer token.
   */

  @Test
  fun testDownloadOkEPUBBearerToken() {
    val task = BorrowDirectDownload.createSubtask()

    Mockito.`when`(this.bookDatabaseEntry.findFormatHandleForContentType(genericEPUBFiles))
      .thenReturn(this.epubHandle)
    Mockito.`when`(this.context.currentURI())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentURICheck())
      .thenReturn(this.webServer.url("/book.epub").toUri())
    Mockito.`when`(this.context.currentAcquisitionPathElement)
      .thenReturn(OPDSAcquisitionPathElement(
        genericEPUBFiles,
        null
      ))

    var savedData = ""
    Mockito.`when`(this.epubHandle.copyInBook(this.anyNonNull()))
      .then {
        savedData = (it.getArgument(0) as File).readText()
        Unit
      }

    val response0 =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", LSHTTPBearerTokenInterceptors.bearerTokenContentType)
        .setBody("""{
          "access_token": "abcd",
          "expires_in": 1000,
          "location": "http://localhost:20000/book.epub"
        }""".trimIndent())

    val response1 =
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/epub+zip")
        .setBody("EPUB!")

    this.webServer.enqueue(response0)
    this.webServer.enqueue(response1)

    task.execute(this.context)

    val sent0 = this.webServer.takeRequest()
    assertEquals(null, sent0.getHeader("Authorization"))
    val sent1 = this.webServer.takeRequest()
    assertEquals("Bearer abcd", sent1.getHeader("Authorization"))
    assertEquals("EPUB!", savedData)
  }
}
