package org.nypl.simplified.books.profiles;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;

import java.io.File;
import java.util.SortedMap;

/**
 * <p>The interface exposed by the profiles database.</p>
 * <p>
 * A profile database stores all of the profiles currently known to the application. It also stores
 * a reference to the current profile. Exactly one profile may be current at any given time. It is
 * the responsibility of the application to have the user select a profile when the application
 * starts, or to create a select a default profile if the application does not require profile
 * functionality.
 * </p>
 * <p>
 * Profiles are an optional feature. However, in order to avoid having to have two entirely
 * separate code paths for the "profiles enabled" and "profiles disabled" cases, implementations
 * of the profiles database can expose an <i>anonymous profile</i>. Applications that want to work
 * with profiles disabled simply create an <i>anonymous profile</i> on startup, and do not allow the
 * user to switch profiles.
 * </p>
 */

public interface ProfilesDatabaseType {

  /**
   * An indicator of whether or not the anonymous profile is enabled.
   */

  enum AnonymousProfileEnabled {

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
   * @return {@link AnonymousProfileEnabled#ANONYMOUS_PROFILE_ENABLED} if the anonymous profile is enabled
   */

  AnonymousProfileEnabled anonymousProfileEnabled();

  /**
   * Access the anonymous profile, or raise an exception if anonymous profiles are not enabled.
   *
   * @return The anonymous profile
   * @throws ProfileAnonymousDisabledException If anonymous profiles are not enabled
   * @see #anonymousProfileEnabled()
   */

  ProfileType anonymousProfile()
      throws ProfileAnonymousDisabledException;

  /**
   * @return The directory containing the on-disk profiles database
   */

  File directory();

  /**
   * @return A read-only view of the current profiles
   */

  SortedMap<ProfileID, ProfileType> profiles();

  /**
   * Create a profile using the given account provider and display name.
   *
   * @param account_provider The account provider for the default account
   * @param display_name     The display name
   * @return A newly created profile
   * @throws ProfileDatabaseException On profile creation errors
   */

  ProfileType createProfile(
      AccountProvider account_provider,
      String display_name)
      throws ProfileDatabaseException;

  /**
   * Find the profile with the given display name, if any.
   *
   * @param display_name The display name
   * @return The profile with the display name, or nothing if one does not exist
   */

  OptionType<ProfileType> findProfileWithDisplayName(
      String display_name);

  /**
   * Set the profile with the given ID as the current profile. Setting the current profile is
   * forbidden if the anonymous profile is enabled.
   *
   * @param profile The profile ID
   * @throws ProfileNonexistentException      If no profile exists with the given ID
   * @throws ProfileAnonymousEnabledException If the anonymous profile is enabled
   * @see #anonymousProfileEnabled()
   */

  void setProfileCurrent(
      ProfileID profile)
      throws ProfileAnonymousEnabledException, ProfileNonexistentException;

  /**
   * Return the current profile. The current profile is the profile set with the most recent
   * call to {@link #setProfileCurrent(ProfileID)}. If the anonymous profile is enabled, this
   * method will always return the anonymous profile.
   *
   * @return The current profile, if any
   */

  OptionType<ProfileType> currentProfile();

  /**
   * Behaves identically to {@link #currentProfile()} but raises {@link ProfileNoneCurrentException}
   * if {@link #currentProfile()} would return {@link com.io7m.jfunctional.None}.
   *
   * @return The current profile
   */

  ProfileType currentProfileUnsafe()
      throws ProfileNoneCurrentException;
}
