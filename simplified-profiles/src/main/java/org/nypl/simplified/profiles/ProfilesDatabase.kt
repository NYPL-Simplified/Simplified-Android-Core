package org.nypl.simplified.profiles

import android.content.Context
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.subjects.Subject
import org.joda.time.LocalDateTime
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.profiles.api.ProfileAnonymousDisabledException
import org.nypl.simplified.profiles.api.ProfileAnonymousEnabledException
import org.nypl.simplified.profiles.api.ProfileCreateDuplicateException
import org.nypl.simplified.profiles.api.ProfileCreateInvalidException
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileNonexistentException
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.SortedMap
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap
import javax.annotation.concurrent.GuardedBy

/**
 * The default implementation of the [ProfilesDatabaseType] interface.
 */

internal class ProfilesDatabase internal constructor(
  private val accountBundledCredentials: AccountBundledCredentialsType,
  private val accountEvents: Subject<AccountEvent>,
  private val accountProviders: AccountProviderRegistryType,
  private val accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
  private val accountsDatabases: AccountsDatabaseFactoryType,
  private val bookFormatSupport: BookFormatSupportType,
  private val analytics: AnalyticsType,
  private val anonymousProfileEnabled: ProfilesDatabaseType.AnonymousProfileEnabled,
  private val context: Context,
  private val directory: File,
  private val profiles: ConcurrentSkipListMap<ProfileID, Profile>
) : ProfilesDatabaseType {

  private val logger = LoggerFactory.getLogger(ProfilesDatabase::class.java)

  private val profilesReadOnly: SortedMap<ProfileID, ProfileType>
  private val profileCurrentLock: Any = Any()
  @GuardedBy("profileCurrentLock")
  private var profileCurrent: ProfileID? = null

  /**
   * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
   * `V <: VB`.
   */

  private fun <K, VB, V : VB> castMap(m: SortedMap<K, V>): SortedMap<K, VB> {
    return m as SortedMap<K, VB>
  }

  init {
    this.profilesReadOnly = castMap(Collections.unmodifiableSortedMap(this.profiles))
    this.profileCurrent = null

    for (profile in this.profiles.values) {
      profile.setOwner(this)
    }
  }

  internal val defaultAccountProvider: AccountProviderType =
    this.accountProviders.defaultProvider

  override fun anonymousProfileEnabled(): ProfilesDatabaseType.AnonymousProfileEnabled {
    return this.anonymousProfileEnabled
  }

  @Throws(ProfileAnonymousDisabledException::class)
  override fun anonymousProfile(): ProfileType {
    return when (this.anonymousProfileEnabled) {
      ANONYMOUS_PROFILE_ENABLED ->
        this.profiles[ProfilesDatabases.ANONYMOUS_PROFILE_ID]!!
      ANONYMOUS_PROFILE_DISABLED ->
        throw ProfileAnonymousDisabledException("The anonymous profile is not enabled")
    }
  }

  override fun directory(): File {
    return this.directory
  }

  override fun profiles(): SortedMap<ProfileID, ProfileType> {
    return this.profilesReadOnly
  }

  @Throws(ProfileDatabaseException::class)
  override fun createProfile(
    accountProvider: AccountProviderType,
    displayName: String
  ): ProfileType {
    if (displayName.isEmpty()) {
      throw ProfileCreateInvalidException("Display name cannot be empty")
    }

    val existing = this.findProfileWithDisplayName(displayName)
    if (existing.isSome) {
      throw ProfileCreateDuplicateException("Display name is already used by an existing profile")
    }

    val next = ProfileID(UUID.randomUUID())

    Preconditions.checkArgument(
      !this.profiles.containsKey(next),
      "Profile ID %s cannot have been used", next
    )

    val profile =
      ProfilesDatabases.createProfileActual(
        context = this.context,
        analytics = this.analytics,
        accountBundledCredentials = this.accountBundledCredentials,
        accountEvents = this.accountEvents,
        accountProviders = this.accountProviders,
        accountsDatabases = this.accountsDatabases,
        accountCredentialsStore = this.accountCredentialsStore,
        accountProvider = accountProvider,
        bookFormatSupport = this.bookFormatSupport,
        directory = this.directory,
        displayName = displayName,
        id = next
      )

    this.profiles[profile.id] = profile
    profile.setOwner(this)

    logProfileCreated(profile)
    return profile
  }

  override fun findProfileWithDisplayName(displayName: String): OptionType<ProfileType> {
    for (profile in this.profiles.values) {
      if (profile.displayName == displayName) {
        return Option.some(profile)
      }
    }
    return Option.none()
  }

  @Throws(ProfileNonexistentException::class, ProfileAnonymousEnabledException::class)
  override fun setProfileCurrent(profile: ProfileID) {
    return when (this.anonymousProfileEnabled) {
      ANONYMOUS_PROFILE_ENABLED -> {
        throw ProfileAnonymousEnabledException(
          "The anonymous profile is enabled; cannot set the current profile"
        )
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        if (!this.profiles.containsKey(profile)) {
          throw ProfileNonexistentException("Profile does not exist")
        }
        this.setCurrentProfile(profile)
      }
    }
  }

  internal fun setCurrentProfile(profile: ProfileID) {
    this.logger.debug("setCurrentProfile: {}", profile)
    synchronized(this.profileCurrentLock) {
      this.profileCurrent = profile
    }
  }

  override fun currentProfile(): OptionType<ProfileType> {
    return synchronized(this.profileCurrentLock) {
      when (this.anonymousProfileEnabled) {
        ANONYMOUS_PROFILE_ENABLED -> {
          try {
            Option.some(this.anonymousProfile())
          } catch (e: ProfileAnonymousDisabledException) {
            throw UnreachableCodeException(e)
          }
        }
        ANONYMOUS_PROFILE_DISABLED -> {
          Option.of<ProfileID>(this.profileCurrent).map { id -> this.profiles[id] }
        }
      }
    }
  }

  @Throws(ProfileNoneCurrentException::class)
  override fun currentProfileUnsafe(): ProfileType {
    return this.currentProfileGet()
  }

  @Throws(ProfileNoneCurrentException::class)
  private fun currentProfileGet(): Profile {
    synchronized(this.profileCurrentLock) {
      val id = this.profileCurrent
      if (id != null) {
        return this.profiles[id]!!
      }
      throw ProfileNoneCurrentException("No profile is current")
    }
  }

  @Throws(IOException::class)
  internal fun deleteProfile(profile: Profile) {
    synchronized(this.profileCurrentLock) {
      this.profiles.remove(profile.id)
      if (this.profileCurrent == profile.id) {
        this.profileCurrent = null
      }

      DirectoryUtilities.directoryDelete(profile.directory)
    }
  }

  private fun logProfileCreated(profile: Profile) {
    this.analytics.publishEvent(
      AnalyticsEvent.ProfileCreated(
        timestamp = LocalDateTime.now(),
        credentials = null,
        profileUUID = profile.id.uuid,
        displayName = profile.displayName,
        birthDate = profile.preferences().dateOfBirth?.show(),
        attributes = profile.description().attributes.attributes
      )
    )
  }
}
