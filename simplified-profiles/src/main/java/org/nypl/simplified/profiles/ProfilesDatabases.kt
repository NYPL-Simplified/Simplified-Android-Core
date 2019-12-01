package org.nypl.simplified.profiles

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseOpenException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.files.FileLocking
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.profiles.api.ProfileDatabaseAccountsException
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileDatabaseIOException
import org.nypl.simplified.profiles.api.ProfileDatabaseOpenException
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.SortedMap
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap

/**
 * Functions providing profile databases.
 */

object ProfilesDatabases {

  private val logger =
    LoggerFactory.getLogger(ProfilesDatabases::class.java)

  val ANONYMOUS_PROFILE_ID =
    ProfileID(UUID(0L, 0L))

  /**
   * Open a profile database from the given directory, creating a new database if one does not
   * exist. The anonymous profile will not be enabled, and will be ignored even if one is present
   * in the on-disk database.
   *
   * @return A profile database
   * @throws ProfileDatabaseException If any errors occurred whilst trying to open the database
   */

  @Throws(ProfileDatabaseException::class)
  fun openWithAnonymousProfileDisabled(
    context: Context,
    analytics: AnalyticsType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountsDatabases: AccountsDatabaseFactoryType,
    directory: File): ProfilesDatabaseType {

    this.logger.debug("opening profile database: {}", directory)

    val profiles = ConcurrentSkipListMap<ProfileID, Profile>()
    val jom = ObjectMapper()

    val errors = ArrayList<Exception>()
    this.openAllProfiles(
      context = context,
      accountEvents = accountEvents,
      accountProviders = accountProviders,
      accountBundledCredentials = accountBundledCredentials,
      accountCredentialsStore = accountCredentialsStore,
      accountsDatabases = accountsDatabases,
      directory = directory,
      profiles = profiles,
      jom = jom,
      errors = errors)
    profiles.remove(this.ANONYMOUS_PROFILE_ID)

    if (errors.isNotEmpty()) {
      for (e in errors) {
        this.logger.error("error during profile database open: ", e)
      }

      throw ProfileDatabaseOpenException(
        "One or more errors occurred whilst trying to open the profile database.",
        errors)
    }

    return ProfilesDatabase(
      analytics = analytics,
      accountBundledCredentials = accountBundledCredentials,
      accountEvents = accountEvents,
      accountProviders = accountProviders,
      accountCredentialsStore = accountCredentialsStore,
      accountsDatabases = accountsDatabases,
      anonymousProfileEnabled = ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED,
      context = context,
      directory = directory,
      profiles = profiles)
  }

  private fun openAllProfiles(
    context: Context,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountsDatabases: AccountsDatabaseFactoryType,
    directory: File,
    profiles: SortedMap<ProfileID, Profile>,
    jom: ObjectMapper,
    errors: MutableList<Exception>) {

    if (!directory.exists()) {
      directory.mkdirs()
    }

    if (!directory.isDirectory) {
      errors.add(IOException("Not a directory: $directory"))
    }

    val profileDirectories = directory.list()
    if (profileDirectories != null) {
      for (profileIdName in profileDirectories) {
        this.logger.debug("opening profile: {}/{}", directory, profileIdName)
        val profile =
          this.openOneProfile(
            context = context,
            accountEvents = accountEvents,
            accountProviders = accountProviders,
            accountsDatabases = accountsDatabases,
            accountBundledCredentials = accountBundledCredentials,
            accountCredentialsStore = accountCredentialsStore,
            jom = jom,
            directory = directory,
            errors = errors,
            profileIdName = profileIdName) ?: continue

        profiles[profile.id] = profile
      }
    }
  }

  /**
   * Open a profile database from the given directory, creating a new database if one does not exist.
   * The anonymous profile will be enabled and will use the given account provider as the default
   * account.
   */

  @Throws(ProfileDatabaseException::class)
  fun openWithAnonymousProfileEnabled(
    context: Context,
    analytics: AnalyticsType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountsDatabases: AccountsDatabaseFactoryType,
    directory: File): ProfilesDatabaseType {

    this.logger.debug("opening profile database: {}", directory)

    val profiles = ConcurrentSkipListMap<ProfileID, Profile>()
    val jom = ObjectMapper()

    val errors = ArrayList<Exception>()
    this.openAllProfiles(
      context = context,
      accountEvents = accountEvents,
      accountProviders = accountProviders,
      accountBundledCredentials = accountBundledCredentials,
      accountCredentialsStore = accountCredentialsStore,
      accountsDatabases = accountsDatabases,
      directory = directory,
      profiles = profiles,
      jom = jom,
      errors = errors)

    if (!profiles.containsKey(this.ANONYMOUS_PROFILE_ID)) {
      val anon =
        this.createProfileActual(
          context = context,
          accountBundledCredentials = accountBundledCredentials,
          accountEvents = accountEvents,
          accountProviders = accountProviders,
          accountsDatabases = accountsDatabases,
          accountCredentialsStore = accountCredentialsStore,
          accountProvider = accountProviders.defaultProvider,
          directory = directory,
          displayName = "",
          id = this.ANONYMOUS_PROFILE_ID)
      profiles[this.ANONYMOUS_PROFILE_ID] = anon
    }

    if (!errors.isEmpty()) {
      throw ProfileDatabaseOpenException(
        "One or more errors occurred whilst trying to open the profile database.", errors)
    }

    val database =
      ProfilesDatabase(
        analytics = analytics,
        accountBundledCredentials = accountBundledCredentials,
        accountEvents = accountEvents,
        accountProviders = accountProviders,
        accountCredentialsStore = accountCredentialsStore,
        accountsDatabases = accountsDatabases,
        anonymousProfileEnabled = ANONYMOUS_PROFILE_ENABLED,
        context = context,
        directory = directory,
        profiles = profiles)

    database.setCurrentProfile(this.ANONYMOUS_PROFILE_ID)
    return database
  }

  private fun openOneProfileDirectory(
    errors: MutableList<Exception>,
    directory: File,
    profileIdName: String): ProfileID? {

    /*
     * If the profile directory is not a directory, then give up.
     */

    val profileDirOld = File(directory, profileIdName)
    if (!profileDirOld.isDirectory) {
      errors.add(IOException("Not a directory: $profileDirOld"))
      return null
    }

    /*
     * Try to parse the existing directory name as a UUID. If it cannot be parsed
     * as a UUID, attempt to rename it to a new UUID value. The reason for this is because
     * profile IDs used to be plain integers, and we have existing deployed clients that are
     * still using those IDs.
     */

    val profileUuid: UUID
    try {
      profileUuid = UUID.fromString(profileIdName)
      return ProfileID(profileUuid)
    } catch (e: Exception) {
      this.logger.warn("could not parse {} as a UUID", profileIdName)
      return this.openOneProfileDirectoryDoMigration(directory, profileDirOld, profileIdName)
    }
  }

  /**
   * Migrate a non-UUID profile directory to a new UUID one.
   */

  private fun openOneProfileDirectoryDoMigration(
    ownerDirectory: File,
    existingDirectory: File,
    profileIdName: String): ProfileID? {

    this.logger.debug("attempting to migrate {} directory", existingDirectory)

    if (profileIdName == "0") {
      val profileDirNew = File(ownerDirectory, this.ANONYMOUS_PROFILE_ID.toString())

      try {
        FileUtilities.fileRename(existingDirectory, profileDirNew)
        this.logger.debug("migrated {} to {}", existingDirectory, profileDirNew)
        return this.ANONYMOUS_PROFILE_ID
      } catch (ex: IOException) {
        this.logger.error("could not migrate directory {} -> {}", existingDirectory, profileDirNew)
        return null
      }
    }

    for (index in 0..99) {
      val profileUUid = UUID.randomUUID()
      val profileDirNew = File(ownerDirectory, profileUUid.toString())
      if (profileDirNew.exists()) {
        continue
      }

      try {
        FileUtilities.fileRename(existingDirectory, profileDirNew)
        this.logger.debug("migrated {} to {}", existingDirectory, profileDirNew)
        return ProfileID(profileUUid)
      } catch (ex: IOException) {
        this.logger.error("could not migrate directory {} -> {}", existingDirectory, profileDirNew)
        return null
      }
    }

    this.logger.error("could not migrate directory {} after multiple attempts, aborting!", existingDirectory)
    return null
  }

  private fun openOneProfile(
    context: Context,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountsDatabases: AccountsDatabaseFactoryType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    jom: ObjectMapper,
    directory: File,
    errors: MutableList<Exception>,
    profileIdName: String): Profile? {

    val profileId =
      this.openOneProfileDirectory(errors, directory, profileIdName) ?: return null

    val profileDir = File(directory, profileId.uuid.toString())
    val profileFile = File(profileDir, "profile.json")
    if (!profileFile.isFile) {
      this.logger.error("[{}]: {} is not a file", profileId.uuid, profileFile)
      return null
    }

    val desc: ProfileDescription
    try {
      desc = ProfileDescriptionJSON.deserializeFromFile(jom, profileFile)
    } catch (e: IOException) {
      errors.add(IOException("Could not parse profile: $profileFile", e))
      return null
    }

    val profileAccountsDir = File(profileDir, "accounts")

    try {
      val accounts =
        accountsDatabases.openDatabase(
          accountAuthenticationCredentialsStore = accountCredentialsStore,
          accountEvents = accountEvents,
          accountProviders = accountProviders,
          context = context,
          directory = profileAccountsDir)

      this.createAutomaticAccounts(
        accounts = accounts,
        accountBundledCredentials = accountBundledCredentials,
        accountProviders = accountProviders,
        profile = profileId)

      if (accounts.accounts().isEmpty()) {
        this.logger.debug("profile is empty, creating a default account")
        accounts.createAccount(accountProviders.defaultProvider)
      }

      Preconditions.checkArgument(
        !accounts.accounts().isEmpty(),
        "Accounts database must not be empty")

      val account = accounts.accounts()[accounts.accounts().firstKey()]!!
      return Profile(null, profileId, profileDir, accounts, desc, account)
    } catch (e: AccountsDatabaseException) {
      errors.add(e)
      return null
    }
  }

  /**
   * Do the actual work of creating the account.
   *
   * @param context            The Android context
   * @param accountProviders  The available account providers
   * @param accountsDatabases A factory for account databases
   * @param accountProvider   The account provider that will be used for the default account
   * @param directory          The profile directory
   * @param displayName       The display name for the account
   * @param id                 The account ID
   */

  @Throws(ProfileDatabaseException::class)
  internal fun createProfileActual(
    context: Context,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountEvents: Subject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountsDatabases: AccountsDatabaseFactoryType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    accountProvider: AccountProviderType,
    directory: File,
    displayName: String,
    id: ProfileID): Profile {

    try {
      val profileDir =
        File(directory, id.uuid.toString())
      val profileAccountsDir =
        File(profileDir, "accounts")
      val prefs =
        ProfilePreferences.builder().build()
      val desc =
        ProfileDescription.builder(displayName, prefs).build()

      try {
        val accounts =
          accountsDatabases.openDatabase(
            accountAuthenticationCredentialsStore = accountCredentialsStore,
            accountEvents = accountEvents,
            accountProviders = accountProviders,
            context = context,
            directory = profileAccountsDir)

        this.createAutomaticAccounts(
          accounts = accounts,
          accountBundledCredentials = accountBundledCredentials,
          accountProviders = accountProviders,
          profile = id)

        val account =
          accounts.createAccount(accountProvider)
        val profile =
          Profile(null, id, profileDir, accounts, desc, account)

        this.writeDescription(profileDir, desc)
        return profile
      } catch (e: AccountsDatabaseException) {
        throw ProfileDatabaseAccountsException("Could not initialize accounts database", e)
      }

    } catch (e: IOException) {
      throw ProfileDatabaseIOException("Could not write profile data", e)
    }
  }

  /**
   * Create an account for all of the providers that are marked as "add automatically".
   */

  @Throws(AccountsDatabaseException::class)
  private fun createAutomaticAccounts(
    accounts: AccountsDatabaseType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountProviders: AccountProviderRegistryType,
    profile: ProfileID) {

    val pId = profile.uuid
    this.logger.debug("[{}]: creating automatic accounts", pId)

    try {
      val autoProviders =
        accountProviders.resolvedProviders.values.filter(AccountProviderType::addAutomatically)

      this.logger.debug("[{}]: {} automatic account providers available", pId, autoProviders.size)

      for (autoProvider in autoProviders) {
        val id = autoProvider.id
        this.logger.debug("[{}]: account provider {} should be added automatically", pId, id)

        val accountsByProvider = accounts.accountsByProvider()
        val autoAccount =
          if (accountsByProvider.containsKey(id)) {
            this.logger.debug("[{}]: automatic account {} already exists", pId, id)
            accountsByProvider[id]!!
          } else {
            this.logger.debug("[{}]: adding automatic account {}", pId, id)
            accounts.createAccount(autoProvider)
          }

        val credentialsOpt =
          accountBundledCredentials.bundledCredentialsFor(id)

        if (credentialsOpt.isSome) {
          this.logger.debug("[{}]: credentials for automatic account {} were provided", pId, id)
          autoAccount.setLoginState(AccountLoginState.AccountLoggedIn(
            (credentialsOpt as Some<AccountAuthenticationCredentials>).get()))
        } else {
          this.logger.debug("[{}]: credentials for automatic account {} were not provided", pId, id)
        }
      }
    } catch (e: Exception) {
      throw AccountsDatabaseOpenException(e.message, listOf(e))
    }
  }

  @Throws(IOException::class)
  internal fun writeDescription(
    directory: File,
    newDescription: ProfileDescription) {

    val profileLock =
      File(directory, "lock")
    val profileFile =
      File(directory, "profile.json")
    val profileFileTemp =
      File(directory, "profile.json.tmp")

    FileLocking.withFileThreadLocked<Unit, IOException>(
      profileLock,
      1000L
    ) { ignored ->

      /*
       * Ignore the return value here; the write call will immediately fail if this
       * call fails anyway.
       */

      directory.mkdirs()

      FileUtilities.fileWriteUTF8Atomically(
        profileFile,
        profileFileTemp,
        ProfileDescriptionJSON.serializeToString(ObjectMapper(), newDescription))
    }
  }
}