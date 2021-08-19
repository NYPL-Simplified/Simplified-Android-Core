package org.nypl.simplified.accounts.database

import android.content.Context

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.io7m.jfunctional.FunctionType
import io.reactivex.subjects.Subject

import net.jcip.annotations.GuardedBy
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountDescription
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseBooksException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseDuplicateProviderException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseIOException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseLastAccountException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseOpenException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseWrongProviderException
import org.nypl.simplified.accounts.json.AccountDescriptionJSON
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileLocking
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.Objects
import java.util.SortedMap
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap

/**
 * The default implementation of the [AccountsDatabaseType] interface.
 */

class AccountsDatabase private constructor(
  private val context: Context,
  private val directory: File,
  private val accountEvents: Subject<AccountEvent>,
  @GuardedBy("accountsLock")
  private val accounts: SortedMap<AccountID, Account>,
  @GuardedBy("accountsLock")
  private val accountsByProvider: SortedMap<URI, Account>,
  private val credentials: AccountAuthenticationCredentialsStoreType,
  private val bookDatabases: BookDatabaseFactoryType,
  private val bookFormatSupport: BookFormatSupportType,
) : AccountsDatabaseType {

  private val logger =
    LoggerFactory.getLogger(AccountsDatabase::class.java)

  private val accountsLock: Any = Any()

  @GuardedBy("accountsLock")
  private val accountsReadOnly: SortedMap<AccountID, AccountType>

  @GuardedBy("accountsLock")
  private val accountsByProviderReadOnly: SortedMap<URI, AccountType>

  init {
    this.accountsReadOnly =
      castMap(Collections.unmodifiableSortedMap(this.accounts))
    this.accountsByProviderReadOnly =
      castMap(Collections.unmodifiableSortedMap(this.accountsByProvider))
  }

  override fun directory(): File {
    return this.directory
  }

  override fun accounts(): SortedMap<AccountID, AccountType> {
    synchronized(this.accountsLock) {
      return this.accountsReadOnly
    }
  }

  override fun accountsByProvider(): SortedMap<URI, AccountType> {
    synchronized(this.accountsLock) {
      return this.accountsByProviderReadOnly
    }
  }

  @Throws(AccountsDatabaseException::class)
  override fun createAccount(accountProvider: AccountProviderType): AccountType {
    val accountId: AccountID
    synchronized(this.accountsLock) {
      if (this.accountsByProvider.containsKey(accountProvider.id)) {
        throw AccountsDatabaseDuplicateProviderException(accountProvider.id.toString())
      }

      accountId = freshAccountID(this.accounts)

      this.logger.debug("creating account {} (provider {})", accountId, accountProvider.id)
      Preconditions.checkArgument(
        !this.accounts.containsKey(accountId),
        "Account ID %s cannot have been used", accountId
      )
    }

    try {
      val accountDir =
        File(this.directory, accountId.toString())
      val accountLock =
        File(accountDir, "lock")
      val accountFile =
        File(accountDir, "account.json")
      val accountFileTemp =
        File(accountDir, "account.json.tmp")
      val booksDir =
        File(accountDir, "books")

      accountDir.mkdirs()

      val bookDatabase =
        this.bookDatabases.openDatabase(
          context = this.context,
          formats = this.bookFormatSupport,
          owner = accountId,
          directory = booksDir
        )

      val preferences =
        AccountPreferences(
          bookmarkSyncingPermitted = false,
          catalogURIOverride = null,
          announcementsAcknowledged = listOf()
        )

      val accountDescription =
        AccountDescription.builder(accountProvider, preferences)
          .build()

      writeDescription(
        accountLock = accountLock,
        accountFile = accountFile,
        accountFileTemp = accountFileTemp,
        description = accountDescription
      )

      val account: Account
      synchronized(this.accountsLock) {
        account =
          Account(
            id = accountId,
            directory = accountDir,
            providerInitial = accountProvider,
            bookDatabase = bookDatabase,
            accountEvents = this.accountEvents,
            description = accountDescription,
            credentials = this.credentials,
            accountLoginState = AccountLoginState.AccountNotLoggedIn
          )
        this.accounts[accountId] = account
        this.accountsByProvider.put(accountProvider.id, account)
      }
      return account
    } catch (e: IOException) {
      throw AccountsDatabaseIOException("Could not write account data", e)
    } catch (e: BookDatabaseException) {
      throw AccountsDatabaseBooksException("Could not create book database", e)
    }
  }

  @Throws(AccountsDatabaseException::class)
  override fun deleteAccountByProvider(accountProvider: URI): AccountID {
    this.logger.debug("delete account by provider: {}", accountProvider)

    synchronized(this.accountsLock) {
      val account =
        this.accountsByProvider[accountProvider]
          ?: throw AccountsDatabaseNonexistentException("No account with the given provider")

      if (this.accounts.size == 1) {
        throw AccountsDatabaseLastAccountException(
          "At least one account must exist at any given time"
        )
      }

      this.accounts.remove(account.id)
      this.accountsByProvider.remove(accountProvider)

      try {
        account.delete()
        DirectoryUtilities.directoryDelete(account.directory())
        return account.id()
      } catch (e: IOException) {
        throw AccountsDatabaseIOException(e.message, e)
      }
    }
  }

  private class Account(
    override val id: AccountID,
    override val directory: File,
    override val bookDatabase: BookDatabaseType,
    private val accountEvents: Subject<AccountEvent>,
    @GuardedBy("descriptionLock")
    private var description: AccountDescription,
    private val credentials: AccountAuthenticationCredentialsStoreType,
    providerInitial: AccountProviderType,
    accountLoginState: AccountLoginState
  ) : AccountType {

    private val logger =
      LoggerFactory.getLogger(Account::class.java)

    private val descriptionLock: Any = Any()

    @GuardedBy("descriptionLock")
    private var loginStateActual: AccountLoginState = accountLoginState

    @GuardedBy("descriptionLock")
    private var providerActual: AccountProviderType = providerInitial

    init {
      this.description =
        Objects.requireNonNull(this.description, "description")
    }

    override fun setAccountProvider(accountProvider: AccountProviderType) {
      this.setDescription(
        FunctionType { existing ->

          val existingProvider = existing.provider()
          if (existingProvider.id != accountProvider.id) {
            throw AccountsDatabaseWrongProviderException(
              "Provider id ${accountProvider.id} does not match the existing id ${existingProvider.id}"
            )
          }
          if (existingProvider.updated.isAfter(accountProvider.updated)) {
            logger.warn(
              "attempted to update provider {} with an older definition",
              existingProvider.id
            )
            return@FunctionType existing
          }

          val modifiedDescription =
            existing.toBuilder()
              .setProvider(accountProvider)
              .build()

          check(accountProvider == modifiedDescription.provider()) {
            "Account providers must match"
          }
          modifiedDescription
        }
      )
    }

    override fun catalogURIForAge(age: Int): URI {
      val catalogURIOverride = this.preferences.catalogURIOverride
      if (catalogURIOverride != null) {
        return catalogURIOverride
      }
      return this.provider.catalogURIForAge(age)
    }

    override val provider: AccountProviderType
      get() = synchronized(this.descriptionLock) {
        return this.providerActual
      }

    fun id(): AccountID {
      return this.id
    }

    fun directory(): File {
      return this.directory
    }

    override val loginState: AccountLoginState
      get() = synchronized(this.descriptionLock) {
        return this.loginStateActual
      }

    override val preferences: AccountPreferences
      get() = synchronized(this.descriptionLock) {
        return this.description.preferences()
      }

    override fun setLoginState(state: AccountLoginState) {
      val oldState = this.loginStateActual

      synchronized(this.descriptionLock) {
        this.loginStateActual = state
      }

      if (state != oldState) {
        this.accountEvents.onNext(
          AccountEventLoginStateChanged(
            message = "",
            accountID = this.id,
            state = state
          )
        )
      }

      val credentials = state.credentials
      if (credentials != null) {
        this.credentials.put(this.id, credentials)
      } else {
        this.credentials.delete(this.id)
      }
    }

    @Throws(AccountsDatabaseException::class)
    override fun setPreferences(preferences: AccountPreferences) {
      this.setDescription(
        FunctionType { accountDescription ->
          accountDescription.toBuilder().setPreferences(preferences).build()
        }
      )
    }

    @Throws(AccountsDatabaseIOException::class)
    private fun setDescription(mutator: FunctionType<AccountDescription, AccountDescription>) {
      try {
        val newDescription: AccountDescription
        synchronized(this.descriptionLock) {
          newDescription = mutator.call(this.description)

          val accountLock =
            File(this.directory, "lock")
          val accountFile =
            File(this.directory, "account.json")
          val accountFileTemp =
            File(this.directory, "account.json.tmp")

          writeDescription(
            accountLock = accountLock,
            accountFile = accountFile,
            accountFileTemp = accountFileTemp,
            description = newDescription
          )

          this.description = newDescription
          this.providerActual = newDescription.provider()

          check(newDescription == this.description)
          check(newDescription.provider() == this.provider)
        }
        this.accountEvents.onNext(AccountEventUpdated("", this.id))
      } catch (e: IOException) {
        throw AccountsDatabaseIOException("Could not write account data", e)
      }
    }

    @Throws(AccountsDatabaseIOException::class)
    fun delete() {
      try {
        logger.debug("account [{}]: delete: {}", this.id, this.directory)

        var exception: Exception? = null
        try {
          logger.debug("account [{}]: delete book database", this.id)
          this.bookDatabase.delete()
        } catch (e: Exception) {
          exception = this.accumulateOrSuppress(exception, e)
        }

        try {
          logger.debug("account [{}]: delete credentials", this.id)
          this.credentials.delete(this.id)
        } catch (e: Exception) {
          exception = this.accumulateOrSuppress(exception, e)
        }

        try {
          logger.debug("account [{}]: delete directory", this.id)
          DirectoryUtilities.directoryDelete(this.directory)
        } catch (e: Exception) {
          exception = this.accumulateOrSuppress(exception, e)
        }

        if (exception != null) {
          throw AccountsDatabaseIOException(exception.message, IOException(exception))
        }
      } finally {
        logger.debug("account [{}]: deleted", this.id)
      }
    }

    private fun accumulateOrSuppress(
      exception: Exception?,
      next: Exception
    ): Exception {
      val result: Exception
      if (exception != null) {
        result = exception
        exception.addSuppressed(next)
      } else {
        result = next
      }
      return result
    }
  }

  companion object {

    private val logger =
      LoggerFactory.getLogger(AccountsDatabase::class.java)

    private fun freshAccountID(accounts: SortedMap<AccountID, Account>): AccountID {
      for (index in 0..99) {
        val accountId = AccountID.generate()
        if (accounts.containsKey(accountId)) {
          continue
        }
        return accountId
      }

      throw IllegalStateException("Could not generate a fresh account ID after multiple attempts")
    }

    /**
     * Open an accounts database from the given directory, creating a new database if one does not exist.
     *
     * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
     */

    @Throws(AccountsDatabaseException::class)
    fun open(
      context: Context,
      accountEvents: Subject<AccountEvent>,
      bookDatabases: BookDatabaseFactoryType,
      bookFormatSupport: BookFormatSupportType,
      accountCredentials: AccountAuthenticationCredentialsStoreType,
      accountProviders: AccountProviderRegistryType,
      directory: File
    ): AccountsDatabaseType {
      this.logger.debug("opening account database: {}", directory)

      val accounts = ConcurrentSkipListMap<AccountID, Account>()
      val accountsByProvider = ConcurrentSkipListMap<URI, Account>()
      val objectMapper = ObjectMapper()

      val errors = ArrayList<Exception>()
      if (!directory.exists()) {
        directory.mkdirs()
      }

      if (!directory.isDirectory) {
        errors.add(IOException("Not a directory: $directory"))
      }

      this.openAllAccounts(
        accountCredentials = accountCredentials,
        accountEvents = accountEvents,
        accountProviderResolver = { id ->
          this.resolveAccountProvider(accountProviders, accountEvents, id)
        },
        accounts = accounts,
        accountsByProvider = accountsByProvider,
        bookDatabases = bookDatabases,
        bookFormatSupport = bookFormatSupport,
        context = context,
        directory = directory,
        errors = errors,
        objectMapper = objectMapper
      )

      if (!errors.isEmpty()) {
        for (e in errors) {
          this.logger.error("error during account database open: ", e)
        }

        throw AccountsDatabaseOpenException(
          "One or more errors occurred whilst trying to open the account database.", errors
        )
      }

      return AccountsDatabase(
        context = context,
        directory = directory,
        accountEvents = accountEvents,
        accounts = accounts,
        accountsByProvider = accountsByProvider,
        credentials = accountCredentials,
        bookDatabases = bookDatabases,
        bookFormatSupport = bookFormatSupport
      )
    }

    private fun resolveAccountProvider(
      accountProviders: AccountProviderRegistryType,
      accountEvents: Subject<AccountEvent>,
      id: String
    ): AccountProviderType? {
      val description =
        accountProviders.findAccountProviderDescription(URI(id)) ?: return null

      val result =
        accountProviders.resolve(
          { _, message ->
            accountEvents.onNext(AccountEventCreation.AccountEventCreationInProgress(message))
          },
          description
        )

      return when (result) {
        is TaskResult.Success -> result.result
        is TaskResult.Failure -> null
      }
    }

    private fun openAllAccounts(
      accounts: SortedMap<AccountID, Account>,
      accountsByProvider: SortedMap<URI, Account>,
      accountProviderResolver: (String) -> AccountProviderType?,
      accountCredentials: AccountAuthenticationCredentialsStoreType,
      accountEvents: Subject<AccountEvent>,
      bookDatabases: BookDatabaseFactoryType,
      bookFormatSupport: BookFormatSupportType,
      context: Context,
      directory: File,
      errors: MutableList<Exception>,
      objectMapper: ObjectMapper
    ) {
      val accountDirs = directory.list()
      if (accountDirs != null) {
        for (index in accountDirs.indices) {
          val accountIdName = accountDirs[index]
          this.logger.debug("opening account: {}/{}", directory, accountIdName)

          val account =
            this.openOneAccount(
              accountEvents = accountEvents,
              accountIdName = accountIdName,
              accountProviderResolver = accountProviderResolver,
              bookDatabases = bookDatabases,
              bookFormatSupport = bookFormatSupport,
              context = context,
              credentialsStore = accountCredentials,
              directory = directory,
              errors = errors,
              objectMapper = objectMapper
            )

          if (account != null) {
            val existingAccount = accountsByProvider[account.provider.id]
            if (existingAccount != null) {
              val message = StringBuilder(128)
                .append("Multiple accounts using the same provider.")
                .append("\n")
                .append("  Provider: ")
                .append(account.provider.id)
                .append("\n")
                .append("  Existing Account: ")
                .append(existingAccount.id.uuid)
                .append("\n")
                .append("  Opening Account: ")
                .append(account.id.uuid)
                .append("\n")
                .toString()
              this.logger.error("{}", message)

              try {
                account.delete()
              } catch (e: AccountsDatabaseIOException) {
                this.logger.error("could not delete broken account: ", e)
              }

              continue
            }

            accounts[account.id] = account
            accountsByProvider[account.provider.id] = account
          }
        }
      }
    }

    private fun openOneAccountDirectory(
      errors: MutableList<Exception>,
      directory: File,
      accountIdName: String
    ): AccountID? {
      /*
       * If the account directory is not a directory, then give up.
       */

      val accountDirOld = File(directory, accountIdName)
      if (!accountDirOld.isDirectory) {
        errors.add(IOException("Not a directory: $accountDirOld"))
        return null
      }

      /*
       * Try to parse the existing directory name as a UUID. If it cannot be parsed
       * as a UUID, attempt to rename it to a new UUID value. The reason for this is because
       * account IDs used to be plain integers, and we have existing deployed clients that are
       * still using those IDs.
       */

      val accountUuid: UUID
      try {
        accountUuid = UUID.fromString(accountIdName)
        return AccountID(accountUuid)
      } catch (e: Exception) {
        this.logger.warn("could not parse {} as a UUID", accountIdName)
        return this.openOneAccountDirectoryDoMigration(directory, accountDirOld)
      }
    }

    /**
     * Migrate a non-UUID account directory to a new UUID one.
     */

    private fun openOneAccountDirectoryDoMigration(
      ownerDirectory: File,
      existingDirectory: File
    ): AccountID? {
      this.logger.debug("attempting to migrate {} directory", existingDirectory)

      for (index in 0..99) {
        val accountUuid = UUID.randomUUID()
        val accountDirNew = File(ownerDirectory, accountUuid.toString())
        if (accountDirNew.exists()) {
          continue
        }

        try {
          FileUtilities.fileRename(existingDirectory, accountDirNew)
          this.logger.debug("migrated {} to {}", existingDirectory, accountDirNew)
          return AccountID(accountUuid)
        } catch (ex: IOException) {
          this.logger.error(
            "could not migrate directory {} -> {}",
            existingDirectory,
            accountDirNew
          )
          return null
        }
      }

      this.logger.error(
        "could not migrate directory {} after multiple attempts, aborting!",
        existingDirectory
      )
      return null
    }

    /**
     * Open a single account.
     */

    private fun openOneAccount(
      accountEvents: Subject<AccountEvent>,
      accountIdName: String,
      accountProviderResolver: (String) -> AccountProviderType?,
      bookDatabases: BookDatabaseFactoryType,
      bookFormatSupport: BookFormatSupportType,
      context: Context,
      credentialsStore: AccountAuthenticationCredentialsStoreType,
      directory: File,
      errors: MutableList<Exception>,
      objectMapper: ObjectMapper
    ): Account? {
      val accountId =
        this.openOneAccountDirectory(errors, directory, accountIdName) ?: return null
      val accountDir =
        File(directory, accountId.toString())
      val accountFile =
        File(accountDir, "account.json")
      val booksDir =
        File(accountDir, "books")

      return try {
        val bookDatabase =
          bookDatabases.openDatabase(
            context = context,
            formats = bookFormatSupport,
            owner = accountId,
            directory = booksDir
          )

        val accountDescription =
          AccountDescriptionJSON.deserializeFromFile(
            objectMapper,
            accountProviderResolver,
            accountFile
          )
        val accountProvider =
          accountDescription.provider()
        val credentials =
          credentialsStore.get(accountId)

        val loginState: AccountLoginState
        if (credentials != null) {
          loginState = AccountLoginState.AccountLoggedIn(credentials)
        } else {
          loginState = AccountLoginState.AccountNotLoggedIn
        }

        val account =
          Account(
            accountEvents = accountEvents,
            accountLoginState = loginState,
            bookDatabase = bookDatabase,
            credentials = credentialsStore,
            description = accountDescription,
            directory = accountDir,
            id = accountId,
            providerInitial = accountProvider
          )

        /*
         * Explicitly set the account provider in order to trigger a re-serialization of on-disk
         * data. This will effectively perform a migration from older versions of the data to newer
         * versions.
         */

        account.setAccountProvider(accountProvider)
        account
      } catch (e: Exception) {
        this.logger.error("could not open account: {}: ", accountFile, e)
        errors.add(IOException("Could not parse account: $accountFile", e))
        this.logger.debug("deleting {}", accountDir)
        DirectoryUtilities.directoryDelete(accountDir)
        null
      }
    }

    /**
     * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
     * `V <: VB`.
     */

    private fun <K, VB, V : VB> castMap(m: SortedMap<K, V>): SortedMap<K, VB> {
      return m as SortedMap<K, VB>
    }

    @Throws(IOException::class)
    private fun writeDescription(
      accountLock: File,
      accountFile: File,
      accountFileTemp: File,
      description: AccountDescription
    ) {
      FileLocking.withFileThreadLocked<Unit, IOException>(
        accountLock, 1000L
      ) {
        FileUtilities.fileWriteUTF8Atomically(
          accountFile,
          accountFileTemp,
          AccountDescriptionJSON.serializeToString(ObjectMapper(), description)
        )
      }
    }
  }
}
