package org.nypl.simplified.profiles

import com.io7m.jfunctional.Option
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.profiles.api.ProfileCreateDuplicateException
import org.nypl.simplified.profiles.api.ProfileDatabaseDeleteAnonymousException
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Objects
import java.util.SortedMap
import javax.annotation.concurrent.GuardedBy

/**
 * A single entry in the profiles database.
 */

internal class Profile internal constructor(
  private var owner: ProfilesDatabase?,
  override val id: ProfileID,
  override val directory: File,
  private val accounts: AccountsDatabaseType,
  initialDescription: ProfileDescription,
  initialAccountCurrent: AccountType) : ProfileType {

  private val logger = LoggerFactory.getLogger(Profile::class.java)

  private val descriptionLock: Any = Any()
  @GuardedBy("descriptionLock")
  private var description: ProfileDescription = initialDescription

  private val accountCurrentLock: Any = Any()
  @GuardedBy("accountCurrentLock")
  private var accountCurrent: AccountType = initialAccountCurrent

  internal fun setOwner(owner: ProfilesDatabase) {
    this.owner = owner
  }

  override val isAnonymous: Boolean
    get() = this.id == ProfilesDatabases.ANONYMOUS_PROFILE_ID

  override val isCurrent: Boolean
    get() = this.owner?.currentProfile()?.map { p -> p.id } == Option.some(this.id)

  override val displayName: String
    get() = synchronized(this.descriptionLock) {
      this.description.displayName()
    }

  override fun accountCurrent(): AccountType {
    synchronized(this.accountCurrentLock) {
      return this.accountCurrent
    }
  }

  override fun accounts(): SortedMap<AccountID, AccountType> {
    return this.accounts.accounts()
  }

  override fun preferences(): ProfilePreferences {
    synchronized(this.descriptionLock) {
      return this.description.preferences()
    }
  }

  override fun accountsByProvider(): SortedMap<URI, AccountType> {
    return this.accounts.accountsByProvider()
  }

  @Throws(AccountsDatabaseNonexistentException::class)
  override fun account(accountId: AccountID): AccountType {
    return this.accounts()[accountId]
      ?: throw AccountsDatabaseNonexistentException("Nonexistent account: $accountId")
  }

  override fun accountsDatabase(): AccountsDatabaseType {
    return this.accounts
  }

  @Throws(IOException::class)
  override fun preferencesUpdate(preferences: ProfilePreferences) {
    val newDescription: ProfileDescription
    synchronized(this.descriptionLock) {
      newDescription = this.description.toBuilder()
        .setPreferences(preferences)
        .build()

      ProfilesDatabases.writeDescription(this.directory, newDescription)
      this.description = newDescription
    }
  }

  override fun setDisplayName(newName: String) {
    val newNameNormal = normalizeDisplayName(newName)
    synchronized(this.descriptionLock) {
      val existing = this.owner!!.findProfileWithDisplayName(newNameNormal)

      /*
         * If a profile exists with the given name, and it's not this profile... Abort!
         */

      if (existing.isSome) {
        if (existing != Option.of(this)) {
          throw ProfileCreateDuplicateException(
            "A profile already exists with the name '$newNameNormal'")
        }
      }

      val newDescription =
        this.description.toBuilder()
          .setDisplayName(newNameNormal)
          .build()

      ProfilesDatabases.writeDescription(this.directory, newDescription)
      this.description = newDescription
    }
  }

  private fun normalizeDisplayName(newName: String): String {
    return newName.trim()
  }

  @Throws(AccountsDatabaseException::class)
  override fun createAccount(accountProvider: AccountProviderType): AccountType {
    return this.accounts.createAccount(accountProvider)
  }

  @Throws(AccountsDatabaseException::class)
  override fun deleteAccountByProvider(accountProvider: URI): AccountID {
    val deleted = this.accounts.deleteAccountByProvider(accountProvider)
    synchronized(this.accountCurrentLock) {
      if (this.accountCurrent.id == deleted) {
        this.accountCurrent = Objects.requireNonNull<AccountType>(this.accounts()[this.accounts().firstKey()])
      }
      return deleted
    }
  }

  @Throws(AccountsDatabaseNonexistentException::class)
  override fun selectAccount(accountProvider: URI): AccountType {
    val account = this.accounts.accountsByProvider()[accountProvider]
    if (account != null) {
      this.setAccountCurrent(account.id)
      return account
    }

    throw AccountsDatabaseNonexistentException(
      "No account with provider: $accountProvider")
  }

  override fun compareTo(other: ProfileReadableType): Int {
    return this.displayName.compareTo(other.displayName)
  }

  @Throws(AccountsDatabaseNonexistentException::class)
  internal fun setAccountCurrent(id: AccountID) {
    synchronized(this.accountCurrentLock) {
      val account = this.accounts.accounts()[id]
      if (account != null) {
        this.accountCurrent = account
      } else {
        throw AccountsDatabaseNonexistentException("No such account: $id")
      }
    }
  }

  override fun delete() {
    this.logger.debug("[{}]: delete", this.id.uuid)

    if (this.isAnonymous) {
      throw ProfileDatabaseDeleteAnonymousException("Cannot delete the anonymous profile")
    }

    this.owner?.deleteProfile(this)
  }
}