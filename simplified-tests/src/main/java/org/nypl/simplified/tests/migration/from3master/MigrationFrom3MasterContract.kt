package org.nypl.simplified.tests.migration.from3master

import android.content.Context
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookFormat.*
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.BookDatabases
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.*
import org.nypl.simplified.migration.from3master.EnvironmentQueriesType
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterProvider
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterStringResourcesType
import org.nypl.simplified.migration.spi.MigrationNotice
import org.nypl.simplified.migration.spi.MigrationNotice.*
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import java.io.File
import java.util.UUID
import kotlin.random.Random

abstract class MigrationFrom3MasterContract {

  private lateinit var tempBookDatabaseDir: File
  private lateinit var services: MigrationServiceDependencies
  private lateinit var tempDir: File
  private lateinit var queries: EnvironmentQueriesType
  private lateinit var migrations: MigrationFrom3MasterProvider
  private lateinit var context: Context

  protected abstract val logger: Logger

  @Before
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

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = { TaskResult.Failure(listOf()) },
        context = this.context)

    this.tempDir.delete()
    this.tempDir.mkdirs()
    this.tempBookDatabaseDir.delete()
    this.tempBookDatabaseDir.mkdirs()
  }

  private class MockStrings : MigrationFrom3MasterStringResourcesType {

    override fun errorBookUnexpectedFormat(title: String, receivedFormat: String): String =
      "errorBookUnexpectedFormat: $title $receivedFormat"

    override fun reportCreatedAccount(title: String): String =
      "reportCreatedAccount: $title"

    override fun reportCopiedBook(title: String): String =
      "reportCopiedBook: $title"

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
    Assert.assertEquals(false, migration.needsToRun())
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
    Assert.assertEquals(true, migration.needsToRun())
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
        applicationProfileIsAnonymous = false,
        createAccount = { TaskResult.Failure(listOf()) },
        context = this.context)

    File(this.tempDir, "salt").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(false, migration.needsToRun())
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
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assert.assertEquals(1, report.notices.size)
    Assert.assertEquals("errorUnknownAccountProvider: 9999", report.notices[0].message)
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
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assert.assertEquals(1, report.notices.size)
    Assert.assertEquals("errorAccountLoadFailure: 12", report.notices[0].message)
    this.logger.debug("exception: ", (report.notices[0] as MigrationError).exception)
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
          val taskRecorder =
            TaskRecorder.create<AccountCreateErrorDetails>()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.currentStepFailed("FAILED!", AccountCreateErrorDetails.UnexpectedException("Ouch", Exception()))
          taskRecorder.finishFailure()
        },
        context = this.context)

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    File(acc, "accounts").mkdirs()
    File(acc, "account.json").writeBytes(this.resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assert.assertEquals(1, report.notices.size)
    Assert.assertEquals("FAILED!", report.notices[0].message)
    this.logger.debug("exception: ", (report.notices[0] as MigrationError).exception)
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

    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder =
            TaskRecorder.create<AccountCreateErrorDetails>()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        context = this.context)

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    File(acc, "accounts").mkdirs()
    File(acc, "books").mkdirs()
    File(File(acc, "books"), "data").mkdirs()
    File(acc, "account.json").writeBytes(this.resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assert.assertEquals(1, report.notices.size)
    Assert.assertEquals("reportCreatedAccount: Account 0", report.notices[0].message)
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
        owner = AccountID(UUID.randomUUID()),
        directory = this.tempBookDatabaseDir)

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Account 0")
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = {
          val taskRecorder =
            TaskRecorder.create<AccountCreateErrorDetails>()
          taskRecorder.beginNewStep("Starting...")
          taskRecorder.finishSuccess(account)
        },
        context = this.context)

    val acc = File(this.tempDir, "12")
    acc.mkdirs()
    File(acc, "accounts").mkdirs()

    val booksDir = File(acc, "books")
    val booksDataDir = File(booksDir, "data")
    val bookDir = File(booksDataDir, "5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    bookDir.mkdirs()

    val bookEPUBFile = File(bookDir, "book.epub")
    val epubData = Random.Default.nextBytes(32)
    bookEPUBFile.writeBytes(epubData)
    val bookMetaFile = File(bookDir, "meta.json")
    bookMetaFile.writeBytes(this.resource("meta0.json"))
    val bookAnnotationsFile = File(bookDir, "annotations.json")
    bookAnnotationsFile.writeBytes(this.resource("annotations0.json"))

    File(acc, "account.json").writeBytes(this.resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    this.showReport(report)
    Assert.assertEquals(2, report.notices.size)
    Assert.assertEquals("reportCreatedAccount: Account 0", report.notices[0].message)
    Assert.assertEquals("reportCopiedBook: Bossypants", report.notices[1].message)

    val bookId = BookID.create("5924cb11000f67c5879f70d0bdfa11cbbd13a3e0feb5a9beda3f4a81032019a0")
    Assert.assertTrue(bookDatabase.books().contains(bookId))
    val format = bookDatabase.entry(bookId).book.findPreferredFormat() as BookFormatEPUB
    Assert.assertEquals(epubData.toList(), format.file!!.readBytes().toList())
  }

  private fun showReport(report: MigrationReport) {
    for (notice in report.notices) {
      when (notice) {
        is MigrationInfo ->
          this.logger.debug("info: {}", notice)
        is MigrationError ->
          this.logger.error("error: {}", notice)
      }
    }
  }

  private fun resource(name: String): ByteArray {
    return MigrationFrom3MasterContract::class.java.getResource(
      "/org/nypl/simplified/tests/migration/from3master/$name")
      .readBytes()
  }
}
