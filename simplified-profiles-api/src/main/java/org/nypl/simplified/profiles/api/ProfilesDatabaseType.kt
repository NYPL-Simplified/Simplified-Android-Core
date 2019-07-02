package org.nypl.simplified.profiles.api

import com.io7m.jfunctional.OptionType

import org.nypl.simplified.accounts.api.AccountProviderType

import java.io.File
import java.util.SortedMap

/**
 * The interface exposed by the profiles database.
 *
 * A profile database stores all of the profiles currently known to the application. It also stores
 * a reference to the current profile. Exactly one profile may be current at any given time. It is
 * the responsibility of the application to have the user select a profile when the application
 * starts, or to create a select a default profile if the application does not require profile
 * functionality.
 *
 * Profiles are an optional feature. However, in order to avoid having to have two entirely
 * separate code paths for the "profiles enabled" and "profiles disabled" cases, implementations
 * of the profiles database can expose an *anonymous profile*. Applications that want to work
 * with profiles disabled simply create an *anonymous profile* on startup, and do not allow the
 * user to switch profiles.
 */

interface ProfilesDatabaseType {

  /**
   * An indicator of whether or not the anonymous profile is enabled.
   */

  enum class AnonymousProfileEnabled {

    /**
     * The anonymous profile is enabled.
     */

    ANONYMOUS_PROFILE_ENABLED,

    /**
     * The anonymous profile is disabled.
     */

    ANONYMOUS_PROFILE_DISABLED
  }

  /**
   * @return [AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED] if the anonymous profile is enabled
   */

  fun anonymousProfileEnabled(): AnonymousProfileEnabled

  /**
   * Access the anonymous profile, or raise an exception if anonymous profiles are not enabled.
   *
   * @return The anonymous profile
   * @throws ProfileAnonymousDisabledException If anonymous profiles are not enabled
   * @see .anonymousProfileEnabled
   */

  @Throws(ProfileAnonymousDisabledException::class)
  fun anonymousProfile(): ProfileType

  /**
   * @return The directory containing the on-disk profiles database
   */

  fun directory(): File

  /**
   * @return A read-only view of the current profiles
   */

  fun profiles(): SortedMap<ProfileID, ProfileType>

  /**
   * Create a profile using the given account provider and display name.
   *
   * @param accountProvider The account provider for the default account
   * @param displayName     The display name
   * @return A newly created profile
   * @throws ProfileDatabaseException On profile creation errors
   */

  @Throws(ProfileDatabaseException::class)
  fun createProfile(
    accountProvider: AccountProviderType,
    displayName: String): ProfileType

  /**
   * Find the profile with the given display name, if any.
   *
   * @param displayName The display name
   * @return The profile with the display name, or nothing if one does not exist
   */

  fun findProfileWithDisplayName(displayName: String): OptionType<ProfileType>

  /**
   * Set the profile with the given ID as the current profile. Setting the current profile is
   * forbidden if the anonymous profile is enabled.
   *
   * @param profile The profile ID
   * @throws ProfileNonexistentException      If no profile exists with the given ID
   * @throws ProfileAnonymousEnabledException If the anonymous profile is enabled
   * @see .anonymousProfileEnabled
   */

  @Throws(ProfileAnonymousEnabledException::class, ProfileNonexistentException::class)
  fun setProfileCurrent(profile: ProfileID)

  /**
   * Return the current profile. The current profile is the profile set with the most recent
   * call to [.setProfileCurrent]. If the anonymous profile is enabled, this
   * method will always return the anonymous profile.
   *
   * @return The current profile, if any
   */

  fun currentProfile(): OptionType<ProfileType>

  /**
   * Behaves identically to [.currentProfile] but raises [ProfileNoneCurrentException]
   * if [.currentProfile] would return [com.io7m.jfunctional.None].
   *
   * @return The current profile
   */

  @Throws(ProfileNoneCurrentException::class)
  fun currentProfileUnsafe(): ProfileType
}
