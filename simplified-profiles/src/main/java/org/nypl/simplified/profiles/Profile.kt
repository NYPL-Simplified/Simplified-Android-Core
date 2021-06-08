package org.nypl.simplified.profiles

import com.io7m.jfunctional.Option
import org.joda.time.LocalDateTime
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.profiles.api.ProfileCreateDuplicateException
import org.nypl.simplified.profiles.api.ProfileDatabaseDeleteAnonymousException
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.SortedMap
import javax.annotation.concurrent.GuardedBy

/**
 * A single entry in the profiles database.
 */

internal class Profile internal constructor(
  private var owner: ProfilesDatabase?,
  override val id: ProfileID,
  override val directory: File,
  private val analytics: AnalyticsType,
  private val accounts: AccountsDatabaseType,
  initialDescription: ProfileDescription
) : ProfileType {

  private val logger = LoggerFactory.getLogger(Profile::class.java)

  @Volatile
  private var deleted: Boolean = false

  private val descriptionLock: Any = Any()
  @GuardedBy("descriptionLock")
  private var descriptionCurrent: ProfileDescription = initialDescription

  internal fun setOwner(owner: ProfilesDatabase) {
    this.owner = owner
  }

  override val isAnonymous: Boolean
    get() = this.id == ProfilesDatabases.ANONYMOUS_PROFILE_ID

  override val isCurrent: Boolean
    get() = this.owner?.currentProfile()?.map { p -> p.id } == Option.some(this.id)

  override fun accounts(): SortedMap<AccountID, AccountType> {
    this.checkNotDeleted()
    return this.accounts.accounts()
  }

  override fun accountsByProvider(): SortedMap<URI, AccountType> {
    this.checkNotDeleted()
    return this.accounts.accountsByProvider()
  }

  @Throws(AccountsDatabaseNonexistentException::class)
  override fun account(accountId: AccountID): AccountType {
    this.checkNotDeleted()
    return this.accounts()[accountId]
      ?: throw AccountsDatabaseNonexistentException("Nonexistent account: $accountId")
  }

  override fun accountsDatabase(): AccountsDatabaseType {
    this.checkNotDeleted()
    return this.accounts
  }

  override fun setDescription(newDescription: ProfileDescription) {
    this.checkNotDeleted()
    synchronized(this.descriptionLock) {
      val newNameNormal =
        this.normalizeDisplayName(newDescription.displayName)
      val existing =
        this.owner!!.findProfileWithDisplayName(newNameNormal)

      /*
       * If a profile exists with the given name, and it's not this profile... Abort!
       */

      if (existing.isSome) {
        if (existing != Option.of(this)) {
          throw ProfileCreateDuplicateException(
            "A profile already exists with the name '$newNameNormal'"
          )
        }
      }

      ProfilesDatabases.writeDescription(this.directory, newDescription)
      this.descriptionCurrent = newDescription
    }

    this.logProfileModified()
  }

  private fun normalizeDisplayName(newName: String): String {
    return newName.trim()
  }

  @Throws(AccountsDatabaseException::class)
  override fun createAccount(accountProvider: AccountProviderType): AccountType {
    this.checkNotDeleted()
    return this.accounts.createAccount(accountProvider)
  }

  @Throws(AccountsDatabaseException::class)
  override fun deleteAccountByProvider(accountProvider: URI): AccountID {
    this.checkNotDeleted()
    val deleted = this.accounts.deleteAccountByProvider(accountProvider)
    val mostRecent = this.descriptionCurrent.preferences.mostRecentAccount
    if (mostRecent == deleted) {
      this.updateMostRecentAccount()
    }
    return deleted
  }

  private fun updateMostRecentAccount() {
    val accounts =
      this.accounts.accounts().values
    val mostRecent =
      if (accounts.size > 1) {
        // Return the first account created from a non-default provider
        accounts.first { it.provider.id != this.owner!!.defaultAccountProvider.id }
      } else {
        // Return the first account
        accounts.first()
      }
    this.setDescription(
      this.descriptionCurrent.copy(
        preferences = this.descriptionCurrent.preferences.copy(
          mostRecentAccount = mostRecent.id
        )
      )
    )
  }

  override fun compareTo(other: ProfileReadableType): Int {
    return this.displayName.compareTo(other.displayName)
  }

  override fun description(): ProfileDescription {
    this.checkNotDeleted()
    return synchronized(this.descriptionLock) {
      this.descriptionCurrent
    }
  }

  private fun checkNotDeleted() {
    check(!this.deleted) { "The profile ${this.id.uuid} has been deleted!" }
  }

  override fun delete() {
    this.logger.debug("[{}]: delete", this.id.uuid)

    if (this.isAnonymous) {
      throw ProfileDatabaseDeleteAnonymousException("Cannot delete the anonymous profile")
    }

    this.logProfileDeleted()
    this.owner?.deleteProfile(this)
    this.deleted = true
  }

  private fun logProfileDeleted() {
    this.analytics.publishEvent(
      AnalyticsEvent.ProfileDeleted(
        timestamp = LocalDateTime.now(),
        credentials = null,
        profileUUID = this.id.uuid,
        displayName = this.displayName,
        birthDate = this.preferences().dateOfBirth?.date?.toString(),
        attributes = this.description().attributes.attributes
      )
    )
  }

  private fun logProfileModified() {
    this.analytics.publishEvent(
      AnalyticsEvent.ProfileUpdated(
        timestamp = LocalDateTime.now(),
        credentials = null,
        profileUUID = this.id.uuid,
        displayName = this.displayName,
        birthDate = this.preferences().dateOfBirth?.date?.toString(),
        attributes = this.description().attributes.attributes
      )
    )
  }
}
