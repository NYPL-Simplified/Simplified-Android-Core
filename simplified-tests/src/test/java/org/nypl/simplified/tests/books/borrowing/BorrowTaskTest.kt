package org.nypl.simplified.tests.books.borrowing

import com.io7m.jfunctional.Option
import org.joda.time.DateTime
import org.joda.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.librarysimplified.http.api.LSHTTPClientType
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.borrowing.BorrowRequest
import org.nypl.simplified.books.borrowing.BorrowRequirements
import org.nypl.simplified.books.borrowing.BorrowTask
import org.nypl.simplified.books.borrowing.BorrowTaskType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.TestDirectories
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class BorrowTaskTest {

  private lateinit var account: AccountType
  private lateinit var accountId: AccountID
  private lateinit var accountProvider: AccountProvider
  private lateinit var book: Book
  private lateinit var bookDatabase: BookDatabaseType
  private lateinit var bookDatabaseEntry: BookDatabaseEntryType
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookID: BookID
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var httpClient: LSHTTPClientType
  private lateinit var opdsEmptyFeedEntry: OPDSAcquisitionFeedEntry
  private lateinit var opdsOpenEPUBFeedEntry: OPDSAcquisitionFeedEntry
  private lateinit var profile: ProfileReadableType
  private lateinit var subtasks: BorrowSubtaskDirectoryType
  private lateinit var temporaryDirectory: File

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
        bookFormatSupport = this.bookFormatSupport,
        bookRegistry = this.bookRegistry,
        clock = { Instant.now() },
        httpClient = this.httpClient,
        profile = this.profile,
        subtasks = this.subtasks,
        temporaryDirectory = this.temporaryDirectory
      ),
      request = request
    )
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  private fun verifyBookRegistryHasFailureStatus() {
    val registryStatus = this.bookRegistry.bookStatusOrNull(this.bookID)!!
    assertEquals(BookStatus.FailedLoan::class.java, registryStatus.javaClass)
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
    this.temporaryDirectory =
      TestDirectories.temporaryDirectory()
    this.bookFormatSupport =
      Mockito.mock(BookFormatSupportType::class.java)
    this.bookRegistry =
      BookRegistry.create()
    this.bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    this.bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
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
    this.httpClient =
      Mockito.mock(LSHTTPClientType::class.java)
    this.subtasks =
      object : BorrowSubtaskDirectoryType {
        override fun findSubtaskFor(
          pathElement: OPDSAcquisitionPathElement
        ): BorrowSubtaskFactoryType? {
          return null
        }
      }

    Mockito.`when`(this.profile.account(this.accountId))
      .thenReturn(this.account)
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)
    Mockito.`when`(this.account.provider)
      .thenReturn(this.accountProvider)
    Mockito.`when`(this.bookDatabase.createOrUpdate(this.anyNonNull(), this.anyNonNull()))
      .thenReturn(this.bookDatabaseEntry)

    Mockito.`when`(this.bookDatabaseEntry.book)
      .then {
        return@then this.book
      }
    Mockito.`when`(this.bookDatabaseEntry.writeOPDSEntry(anyNonNull()))
      .then {
        throw java.lang.IllegalStateException("Not implemented!")
      }

    this.opdsEmptyFeedEntry =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Book",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      ).build()

    this.opdsOpenEPUBFeedEntry =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Book",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      ).addAcquisition(
        OPDSAcquisition(
          OPDSAcquisition.Relation.ACQUISITION_GENERIC,
          URI.create("http://www.example.com"),
          StandardFormatNames.genericEPUBFiles,
          listOf()
        )
      ).build()
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

    this.verifyBookRegistryHasFailureStatus()
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

    this.verifyBookRegistryHasFailureStatus()
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

    this.verifyBookRegistryHasFailureStatus()
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

    Mockito.`when`(this.bookFormatSupport.isSupportedPath(this.anyNonNull()))
      .thenReturn(true)
    Mockito.`when`(this.bookFormatSupport.isSupportedFinalContentType(this.anyNonNull()))
      .thenReturn(true)

    val result = this.executeAssumingFailure(task)
    assertEquals(BorrowErrorCodes.noSubtaskAvailable, result.lastErrorCode)

    this.verifyBookRegistryHasFailureStatus()
  }
}
