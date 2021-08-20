package org.nypl.simplified.tests.migration.from3master

import android.content.Context
import io.reactivex.subjects.PublishSubject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.BookDatabases
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.migration.from3master.EnvironmentQueriesType
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterProvider
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterStringResourcesType
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepError
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepSucceeded
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.books.BookFormatsTesting
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

abstract class MigrationFrom3MasterContract {

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var tempBookDatabaseDir: File
  private lateinit var services: MigrationServiceDependencies
  private lateinit var tempDir: File
  private lateinit var queries: EnvironmentQueriesType
  private lateinit var migrations: MigrationFrom3MasterProvider
  private lateinit var context: Context

  protected abstract val logger: Logger

  @BeforeEach
  fun testSetup() {
    this.context =
      Mockito.mock(Context::class.java)
    this.queries =
      Mockito.mock(EnvironmentQueriesType::class.java)
    this.migrations =
      MigrationFrom3MasterProvider(this.queries)
    this.tempDir =
      File.createTempFile("migration", "dir")
    this.tempBookDatabaseDir =
      File.createTempFile("migrationBookDatabase", "dir")

    this.migrations.setStrings(MockStrings())

    this.accountEvents =
      PublishSubject.create()

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILED!", "unexpectedException")
          taskRecorder.finishFailure()
        },
        loginAccount = { _, _ ->
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILED!", "unexpectedException")
          taskRecorder.finishFailure()
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    this.tempDir.delete()
    this.tempDir.mkdirs()
    this.tempBookDatabaseDir.delete()
    this.tempBookDatabaseDir.mkdirs()
  }

  private class MockStrings : MigrationFrom3MasterStringResourcesType {
    override fun progressLoadingAccount(idNumeric: Int): String =
      "progressLoadingAccount: $idNumeric"

    override fun errorAccountAuthenticationNoCredentials(accountTitle: String): String =
      "errorAccountAuthenticationNoCredentials: $accountTitle"

    override fun successAuthenticatedAccountNotRequired(title: String): String =
      "successAuthenticatedAccountNotRequired: $title"

    override val successDeletedOldData: String =
      "successDeletedOldData"

    override fun errorAccountAuthenticationFailure(title: String): String =
      "errorAccountAuthenticationFailure: $title"

    override fun successAuthenticatedAccount(title: String): String =
      "successAuthenticatedAccount: $title"

    override fun errorBookCopyFailure(title: String): String =
      "errorBookCopyFailure: $title"

    override fun errorBookAdobeDRMCopyFailure(title: String): String =
      "errorBookAdobeDRMCopyFailure: $title"

    override fun errorBookmarksCopyFailure(title: String): String =
      "errorBookmarksCopyFailure: $title"

    override fun successCopiedBookmarks(title: String, count: Int): String =
      "successCopiedBookmarks: $title $count"

    override fun errorBookmarksParseFailure(title: String): String =
      "errorBookmarksParseFailure: $title"

    override fun errorBookUnexpectedFormat(title: String, receivedFormat: String): String =
      "errorBookUnexpectedFormat: $title $receivedFormat"

    override fun successCreatedAccount(title: String): String =
      "successCreatedAccount: $title"

    override fun successCopiedBook(title: String): String =
      "successCopiedBook: $title"

    override fun errorBookLoadFailure(entry: String): String =
      "errorBookLoadFailure: $entry"

    override fun errorBookLoadTitledFailure(title: String): String =
      "errorBookLoadTitledFailure: $title"

    override fun errorAccountLoadFailure(id: Int): String =
      "errorAccountLoadFailure: $id"

    override fun errorUnknownAccountProvider(id: Int): String =
      "errorUnknownAccountProvider: $id"
  }

  /**
   * When none of the expected files exist, the migration doesn't need to run.
   */

  @Test
  fun testNotRequired() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(false, migration.needsToRun())
  }

  /**
   * When one or more of the expected files exist, the migration needs to run.
   */

  @Test
  fun testFilesExist() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    File(this.tempDir, "salt").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())
  }

  /**
   * If the application is not in anonymous profile mode, the migration can't run.
   */

  @Test
  fun testNotAnonymous() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    this.services =
      MigrationServiceDependencies(
        accountEvents = this.accountEvents,
        applicationProfileIsAnonymous = false,
        context = this.context,
        createAccount = { TaskResult.Failure(listOf(), mapOf()) },
        loginAccount = { _, _ -> TaskResult.Failure(listOf(), mapOf()) },
        applicationVersion = "test suite 0.0.1"
      )

    File(this.tempDir, "salt").writeBytes(ByteArray(16))

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  /**
   * An unknown account provider cannot be migrated.
   */

  @Test
  fun testRunUnknownAccountProvider() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val acc = File(this.tempDir, "9999")
    acc.mkdirs()
    File(acc, "accounts").mkdirs()
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(2, report.events.size)
    Assertions.assertEquals("errorUnknownAccountProvider: 9999", report.events[0].message)
    Assertions.assertEquals("successDeletedOldData", report.events[1].message)

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  /**
   * An unparseable account provider cannot be migrated.
   */

  @Test
  fun testRunUnparseableAccountProvider() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    File(acc, "accounts").mkdirs()
    File(acc, "account.json").writeBytes(ByteArray(16))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(3, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("FAILED!", report.events[1].message)
    Assertions.assertEquals("successDeletedOldData", report.events[2].message)
    this.logger.debug("exception: ", (report.events[1] as MigrationStepError).exception)

    this.assertWouldASecondMigrationNeedToRun(true)
  }

  /**
   * If account creation fails, an error is logged.
   */

  @Test
  fun testRunAccountCreationFails() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILED!", "unexpectedException")
          taskRecorder.finishFailure()
        },
        loginAccount = { _, _ ->
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILED!", "unexpectedException")
          taskRecorder.finishFailure()
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    val accountsDir = File(acc, "accounts")
    accountsDir.mkdirs()
    val accountFile = File(accountsDir, "account.json")
    accountFile.writeBytes(this.resource("account.json"))
    val deviceFile = File(this.tempDir, "device.xml")
    deviceFile.writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(3, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("FAILED!", report.events[1].message)
    Assertions.assertEquals("successDeletedOldData", report.events[2].message)

    this.logger.debug("exception: ", (report.events[1] as MigrationStepError).exception)

    /*
     * Because account creation failed, the original files are not removed.
     */

    for (
      file in listOf(
        accountsDir,
        accountFile,
        deviceFile
      )
    ) {
      Assertions.assertTrue(file.exists(), "$file exists")
    }

    this.assertWouldASecondMigrationNeedToRun(true)
  }

  /**
   * A single NYPL account with no books is migrated correctly.
   */

  @Test
  fun testRunAccountNYPLSingle() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(accountProvider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          description = "Basic",
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 20,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          labels = mapOf(),
          logoURI = null
        )
      )
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)

    val accountID = AccountID(UUID.randomUUID())
    Mockito.`when`(account.id)
      .thenReturn(accountID)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        loginAccount = { _, _ ->
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(Unit)
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    val accountsSubdir = File(acc, "accounts")
    accountsSubdir.mkdirs()
    File(acc, "books").mkdirs()
    File(File(acc, "books"), "data").mkdirs()
    File(accountsSubdir, "account.json").writeBytes(this.resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(4, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("successCreatedAccount: Account 0", report.events[1].message)
    Assertions.assertEquals("successAuthenticatedAccount: Account 0", report.events[2].message)
    Assertions.assertEquals("successDeletedOldData", report.events[3].message)

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  /**
   * A single NYPL account with a single book is migrated correctly.
   */

  @Test
  fun testRunAccountNYPLSingleOneBook() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val bookDatabase =
      BookDatabases.openDatabase(
        context = this.context,
        formats = BookFormatsTesting.supportsEverything,
        owner = AccountID(UUID.randomUUID()),
        directory = this.tempBookDatabaseDir
      )

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(accountProvider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          description = "Basic",
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 20,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          labels = mapOf(),
          logoURI = null
        )
      )
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    val accountID = AccountID(UUID.randomUUID())
    Mockito.`when`(account.id)
      .thenReturn(accountID)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder =
            TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        loginAccount = { _, _ ->
          val taskRecorder =
            TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(Unit)
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    val accountsSubDir = File(acc, "accounts")
    accountsSubDir.mkdirs()

    val booksDir = File(acc, "books")
    val booksDataDir = File(booksDir, "data")
    val bookDir =
      File(booksDataDir, "5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    bookDir.mkdirs()

    val bookEPUBFile = File(bookDir, "book.epub")
    val epubData = Random.nextBytes(32)
    bookEPUBFile.writeBytes(epubData)
    val bookMetaFile = File(bookDir, "meta.json")
    bookMetaFile.writeBytes(this.resource("meta0.json"))
    val bookAnnotationsFile = File(bookDir, "annotations.json")
    bookAnnotationsFile.writeBytes(this.resource("annotations0.json"))

    val accountFile = File(accountsSubDir, "account.json")
    accountFile.writeBytes(this.resource("account.json"))
    val deviceFile = File(this.tempDir, "device.xml")
    deviceFile.writeBytes(ByteArray(16))

    for (
      file in listOf(
        booksDir,
        booksDataDir,
        bookDir,
        bookEPUBFile,
        bookMetaFile,
        bookAnnotationsFile,
        accountFile,
        deviceFile
      )
    ) {
      Assertions.assertTrue(file.exists(), "$file no exists")
    }

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(6, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("successCreatedAccount: Account 0", report.events[1].message)
    Assertions.assertEquals("successCopiedBookmarks: Bossypants 1", report.events[2].message)
    Assertions.assertEquals("successCopiedBook: Bossypants", report.events[3].message)
    Assertions.assertEquals("successAuthenticatedAccount: Account 0", report.events[4].message)
    Assertions.assertEquals("successDeletedOldData", report.events[5].message)

    val bookId = BookID.create("5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    Assertions.assertTrue(bookDatabase.books().contains(bookId))
    val format = bookDatabase.entry(bookId).book.findPreferredFormat() as BookFormatEPUB
    Assertions.assertEquals(epubData.toList(), format.file!!.readBytes().toList())

    /*
     * All files should be gone.
     */

    for (
      file in listOf(
        booksDir,
        booksDataDir,
        bookDir,
        bookEPUBFile,
        bookMetaFile,
        bookAnnotationsFile,
        accountsSubDir,
        accountFile,
        deviceFile
      )
    ) {
      Assertions.assertTrue(!file.exists(), "$file no longer exists")
    }

    /*
     * The migration should not want to run if asked a second time.
     */

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  private fun assertWouldASecondMigrationNeedToRun(wouldRun: Boolean) {
    val migrationAfter = this.migrations.create(this.services)
    Assertions.assertEquals(wouldRun, migrationAfter.needsToRun())
  }

  /**
   * Errors are reported for book database failures.
   */

  @Test
  fun testRunAccountNYPLSingleOneBookDatabaseFailures() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val bookDatabaseEntryFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(accountProvider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          description = "Basic",
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 20,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          labels = mapOf(),
          logoURI = null
        )
      )
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    val accountID = AccountID(UUID.randomUUID())
    Mockito.`when`(account.id)
      .thenReturn(accountID)

    val opdsEntry =
      this.opdsEntry("meta0.json")
    val bookId =
      BookID.create("5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")

    Mockito.`when`(bookDatabase.createOrUpdate(bookId, opdsEntry))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseEntryFormatHandle)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder =
            TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        loginAccount = { _, _ ->
          val taskRecorder =
            TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(Unit)
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    val accountsDir = File(acc, "accounts")
    accountsDir.mkdirs()

    val booksDir = File(acc, "books")
    val booksDataDir = File(booksDir, "data")
    val bookDir =
      File(booksDataDir, "5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    bookDir.mkdirs()

    val bookEPUBFile = File(bookDir, "book.epub")
    val epubData = Random.nextBytes(32)
    bookEPUBFile.writeBytes(epubData)
    val bookMetaFile = File(bookDir, "meta.json")
    bookMetaFile.writeBytes(this.resource("meta0.json"))
    val bookAnnotationsFile = File(bookDir, "annotations.json")
    bookAnnotationsFile.writeBytes(this.resource("annotations0.json"))

    File(accountsDir, "account.json").writeBytes(this.resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    Mockito.`when`(bookDatabaseEntryFormatHandle.setBookmarks(Mockito.anyList()))
      .thenThrow(IOException("Bad bookmarks"))
    Mockito.`when`(bookDatabaseEntryFormatHandle.copyInBook(bookEPUBFile))
      .thenThrow(IOException("Bad book"))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(6, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("successCreatedAccount: Account 0", report.events[1].message)
    Assertions.assertEquals("errorBookCopyFailure: Bossypants", report.events[2].message)
    Assertions.assertEquals("errorBookmarksCopyFailure: Bossypants", report.events[3].message)
    Assertions.assertEquals("successAuthenticatedAccount: Account 0", report.events[4].message)
    Assertions.assertEquals("successDeletedOldData", report.events[5].message)

    Assertions.assertFalse(bookDatabase.books().contains(bookId))

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  /**
   * Errors are reported for authentication failures.
   */

  @Test
  fun testRunAccountNYPLAuthenticationFailure() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    val bookDatabase =
      BookDatabases.openDatabase(
        context = this.context,
        formats = BookFormatsTesting.supportsEverything,
        owner = AccountID(UUID.randomUUID()),
        directory = this.tempBookDatabaseDir
      )

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(accountProvider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          description = "Basic",
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 20,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          labels = mapOf(),
          logoURI = null
        )
      )
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    val accountID = AccountID(UUID.randomUUID())
    Mockito.`when`(account.id)
      .thenReturn(accountID)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        loginAccount = { _, _ ->
          val taskRecorder = TaskRecorder.create()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILURE!", "unexpectedException")
          taskRecorder.finishFailure()
        },
        accountEvents = this.accountEvents,
        applicationVersion = "test suite 0.0.1",
        context = this.context
      )

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    val accountsSubDir = File(acc, "accounts")
    accountsSubDir.mkdirs()

    val booksDir = File(acc, "books")
    val booksDataDir = File(booksDir, "data")
    val bookDir =
      File(booksDataDir, "5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    bookDir.mkdirs()

    val bookEPUBFile = File(bookDir, "book.epub")
    val epubData = Random.nextBytes(32)
    bookEPUBFile.writeBytes(epubData)
    val bookMetaFile = File(bookDir, "meta.json")
    bookMetaFile.writeBytes(this.resource("meta0.json"))
    val bookAnnotationsFile = File(bookDir, "annotations.json")
    bookAnnotationsFile.writeBytes(this.resource("annotations0.json"))

    val accountFile = File(accountsSubDir, "account.json")
    accountFile.writeBytes(this.resource("account.json"))
    val deviceFile = File(this.tempDir, "device.xml")
    deviceFile.writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assertions.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assertions.assertEquals(6, report.events.size)
    Assertions.assertEquals("progressLoadingAccount: 12", report.events[0].message)
    Assertions.assertEquals("successCreatedAccount: Account 0", report.events[1].message)
    Assertions.assertEquals("successCopiedBookmarks: Bossypants 1", report.events[2].message)
    Assertions.assertEquals("successCopiedBook: Bossypants", report.events[3].message)
    Assertions.assertEquals("FAILURE!", report.events[4].message)
    Assertions.assertEquals("successDeletedOldData", report.events[5].message)

    val bookId = BookID.create("5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    Assertions.assertTrue(bookDatabase.books().contains(bookId))
    val format = bookDatabase.entry(bookId).book.findPreferredFormat() as BookFormatEPUB
    Assertions.assertEquals(epubData.toList(), format.file!!.readBytes().toList())

    /*
     * Because everything except authentication succeeded, the original files can be deleted.
     */

    for (
      file in listOf(
        booksDir,
        booksDataDir,
        bookDir,
        bookEPUBFile,
        bookMetaFile,
        bookAnnotationsFile,
        accountsSubDir,
        accountFile,
        deviceFile
      )
    ) {
      Assertions.assertTrue(!file.exists(), "$file no longer exists")
    }

    this.assertWouldASecondMigrationNeedToRun(false)
  }

  private fun opdsEntry(name: String): OPDSAcquisitionFeedEntry {
    val parser = OPDSJSONParser.newParser()
    return parser.parseAcquisitionFeedEntryFromStream(ByteArrayInputStream(this.resource(name)))
  }

  private fun showReport(report: MigrationReport) {
    for (notice in report.events) {
      when (notice) {
        is MigrationStepSucceeded ->
          this.logger.debug("info: {}", notice)
        is MigrationStepError ->
          this.logger.error("error: {}", notice)
      }
    }
  }

  private fun resource(name: String): ByteArray {
    return MigrationFrom3MasterContract::class.java.getResource(
      "/org/nypl/simplified/tests/migration/from3master/$name"
    )!!.readBytes()
  }
}
