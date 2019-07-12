package org.nypl.simplified.migration.from3master

import android.content.Context
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.migration.spi.MigrationError
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URI

/**
 * A migration from version 3.0 of the app (2019 pre-LFA master branch).
 */

class MigrationFrom3Master(
  private val environment: EnvironmentQueriesType,
  private val strings: MigrationFrom3MasterStringResourcesType,
  private val services: MigrationServiceDependencies) : MigrationType {

  private val logger =
    LoggerFactory.getLogger(MigrationFrom3Master::class.java)

  /**
   * The old master branch always stored accounts and DRM information on internal storage.
   * Actual books and bookmarks may have been stored on external storaged based on
   * [determineDiskDataDirectory].
   */

  private val oldBaseAccountsDirectory =
    this.services.context.filesDir

  private val objectMapper = ObjectMapper()
  private val errors = mutableListOf<MigrationError>()

  override fun needsToRun(): Boolean {
    if (!this.services.applicationProfileIsAnonymous) {
      this.logger.debug("application is not in anonymous mode, cannot migrate!")
      return false
    }

    val candidates =
      listOf("accounts", "activation.xml", "device", "device.xml", "salt")
        .map { name -> File(this.oldBaseAccountsDirectory, name) }

    return candidates.any { file ->
      if (file.exists()) {
        this.logger.debug("file {} exists, so migration is needed", file)
        true
      } else {
        this.logger.debug("file {} does not exist", file)
        false
      }
    }
  }

  data class EnumeratedAccount(
    val baseDirectory: File,
    val adobeDeviceXML: File?,
    val idURI: URI,
    val idNumeric: Int)

  data class LoadedAccount(
    val enumeratedAccount: EnumeratedAccount,
    val account: MigrationFrom3MasterAccount)

  data class CreatedAccount(
    val loadedAccount: LoadedAccount,
    val account: AccountType)

  override fun run(): MigrationReport {
    val accounts = this.enumerateAccountsToMigrate()
    this.logger.debug("{} accounts to migrate", accounts.size)
    val loadedAccounts = this.loadAccounts(accounts)
    this.logger.debug("{} accounts loaded", loadedAccounts.size)
    val createdAccounts = this.createAccounts(loadedAccounts)
    this.logger.debug("{} accounts created", createdAccounts.size)
    return MigrationReport(errors = this.errors)
  }

  private fun createAccounts(accounts: List<LoadedAccount>): List<CreatedAccount> {
    val createdAccounts = mutableListOf<CreatedAccount>()
    for (account in accounts) {
      this.createAccount(account)?.let { createdAccounts.add(it) }
    }
    return createdAccounts.toList()
  }

  private fun createAccount(account: LoadedAccount): CreatedAccount? {
    return when (val taskResult =
      this.services.createAccount(account.enumeratedAccount.idURI)) {
      is TaskResult.Success -> {
        CreatedAccount(account, taskResult.result)
      }
      is TaskResult.Failure -> {
        val message = taskResult.steps.last().resolution.message
        val causes =
          taskResult.steps.mapNotNull { step ->
            when (val resolution = step.resolution) {
              is TaskStepResolution.TaskStepSucceeded -> null
              is TaskStepResolution.TaskStepFailed -> resolution.errorValue
            }
          }

        this.errors.add(MigrationError(message = message, causes = causes))
        null
      }
    }
  }

  private fun loadAccounts(accounts: List<EnumeratedAccount>): List<LoadedAccount> {
    val loadedAccounts = mutableListOf<LoadedAccount>()
    for (account in accounts) {
      this.loadAccount(account)?.let { loadedAccounts.add(it) }
    }
    return loadedAccounts.toList()
  }

  /**
   * Try to load a single account.
   */

  private fun loadAccount(account: EnumeratedAccount): LoadedAccount? {
    return try {
      val accountFile = File(account.baseDirectory, "account.json")
      val accountData = FileInputStream(accountFile).use { stream ->
        this.objectMapper.readValue(stream, MigrationFrom3MasterAccount::class.java)
      }
      LoadedAccount(account, accountData)
    } catch (e: java.lang.Exception) {
      this.logger.error("could not load account: ", e)
      this.errors.add(MigrationError(
        message = this.strings.errorAccountLoadFailure(account.idNumeric),
        attributes = mapOf(Pair("idNumeric", account.idNumeric.toString())),
        exception = e))
      null
    }
  }

  private fun enumerateAccountsToMigrate(): List<EnumeratedAccount> {
    val enumeratedAccounts = mutableListOf<EnumeratedAccount>()

    /*
     * For reasons unknown to anyone, NYPL accounts (and _only_ NYPL accounts) weren't placed
     * in a numbered directory. They were simply placed directly in the base directory to ensure
     * that every bit of code in the app that dealt with directories had to special case them.
     */

    val nyplAccounts = File(this.oldBaseAccountsDirectory, "accounts")
    if (nyplAccounts.isDirectory) {
      this.enumerateAccount(
        accountDirectory = this.oldBaseAccountsDirectory,
        idNumeric = 0)
        ?.let { enumeratedAccounts.add(it) }
    }

    /*
     * For non NYPL accounts, each account has a numbered directory where the name of the
     * directory corresponds to the integer ID of the account provider. For each directory
     * that appears to be a number, we assume it's an account and try to look up the corresponding
     * provider.
     */

    val baseFiles = this.oldBaseAccountsDirectory.list()
    if (baseFiles != null) {
      for (file in baseFiles) {
        val id = try {
          file.toInt()
        } catch (e: NumberFormatException) {
          null
        }

        if (id != null) {
          this.enumerateAccount(
            accountDirectory = File(this.oldBaseAccountsDirectory, id.toString()),
            idNumeric = id)
            ?.let { enumeratedAccounts.add(it) }
        }
      }
    }

    return enumeratedAccounts.toList()
  }

  private fun enumerateAccount(
    accountDirectory: File,
    idNumeric: Int
  ): EnumeratedAccount? {
    val adobeDeviceXML = File(this.oldBaseAccountsDirectory, "device.xml")
    val idURI = MigrationFrom3MasterProviders.providers[idNumeric]

    if (idURI == null) {
      this.logger.error("no account provider for id $idNumeric")
      this.errors.add(MigrationError(
        message = this.strings.errorUnknownAccountProvider(idNumeric),
        attributes = mapOf(Pair("idNumeric", idNumeric.toString())),
        exception = Exception()))
      return null
    }

    this.logger.debug("will migrate account $idNumeric -> $idURI")
    return EnumeratedAccount(
      baseDirectory = accountDirectory,
      adobeDeviceXML = this.isFileOrNull(adobeDeviceXML),
      idURI = idURI,
      idNumeric = idNumeric)
  }

  private fun isFileOrNull(file: File): File? {
    return if (file.isFile) {
      file
    } else {
      null
    }
  }

  /**
   * Try to determine which directory will be used to hold data.
   */

  private fun determineDiskDataDirectory(context: Context): File {

    /*
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for data.
     */

    if (Environment.MEDIA_MOUNTED == this.environment.getExternalStorageState()) {
      this.logger.debug("trying external storage")
      if (!this.environment.isExternalStorageRemovable()) {
        val result = context.getExternalFilesDir(null)
        this.logger.debug("external storage is not removable, using it ({})", result)
        Preconditions.checkArgument(result!!.isDirectory, "Data directory {} is a directory", result)
        return result
      }
    }

    /*
     * Otherwise, use internal storage.
     */

    val result = context.filesDir
    this.logger.debug("no non-removable external storage, using internal storage ({})", result)
    Preconditions.checkArgument(result.isDirectory, "Data directory {} is a directory", result)
    return result
  }
}
