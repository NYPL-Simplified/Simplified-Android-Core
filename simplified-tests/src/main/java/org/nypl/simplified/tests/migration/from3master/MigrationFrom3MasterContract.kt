package org.nypl.simplified.tests.migration.from3master

import android.content.Context
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.migration.from3master.EnvironmentQueriesType
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterProvider
import org.nypl.simplified.migration.from3master.MigrationFrom3MasterStringResourcesType
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import java.io.File

abstract class MigrationFrom3MasterContract {

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

    this.migrations.setStrings(MockStrings())

    this.services =
      MigrationServiceDependencies(
        applicationProfileIsAnonymous = true,
        createAccount = { TaskResult.Failure(listOf()) },
        context = this.context)

    this.tempDir.delete()
    this.tempDir.mkdirs()
  }

  private class MockStrings : MigrationFrom3MasterStringResourcesType {
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
   * Migration works for an almost empty directory.
   */

  @Test
  fun testRunMinimal() {
    Mockito.`when`(this.queries.getExternalStorageState())
      .thenReturn("UNKNOWN")
    Mockito.`when`(this.context.filesDir)
      .thenReturn(this.tempDir)

    File(this.tempDir, "accounts").mkdirs()
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
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
    Assert.assertEquals(1, report.errors.size)
    Assert.assertEquals("errorUnknownAccountProvider: 9999", report.errors[0].message)
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
    Assert.assertEquals(1, report.errors.size)
    Assert.assertEquals("errorAccountLoadFailure: 12", report.errors[0].message)
    this.logger.debug("exception: ", report.errors[0].exception)
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
    File(acc, "account.json").writeBytes(resource("account.json"))
    File(this.tempDir, "device.xml").writeBytes(ByteArray(16))

    val migration = this.migrations.create(this.services)
    Assert.assertEquals(true, migration.needsToRun())

    val report = migration.run()
    Assert.assertEquals(1, report.errors.size)
    Assert.assertEquals("FAILED!", report.errors[0].message)
    this.logger.debug("exception: ", report.errors[0].exception)
  }

  private fun resource(name: String): ByteArray {
    return MigrationFrom3MasterContract::class.java.getResource(
      "/org/nypl/simplified/tests/migration/from3master/$name")
      .readBytes()
  }
}
