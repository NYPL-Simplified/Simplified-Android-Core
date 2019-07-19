package org.nypl.simplified.profiles.controller.api;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FluentFuture;
import com.io7m.jfunctional.Unit;

import org.joda.time.LocalDate;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails;
import org.nypl.simplified.accounts.api.AccountDeleteErrorDetails;
import org.nypl.simplified.accounts.api.AccountEvent;
import org.nypl.simplified.accounts.api.AccountEventCreation;
import org.nypl.simplified.accounts.api.AccountEventDeletion;
import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.api.AccountLoginState;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.api.BookID;
import org.nypl.simplified.feeds.api.Feed;
import org.nypl.simplified.observable.ObservableReadableType;
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent;
import org.nypl.simplified.profiles.api.ProfileCreationEvent;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.ProfileID;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.profiles.api.ProfilePreferences;
import org.nypl.simplified.profiles.api.ProfileReadableType;
import org.nypl.simplified.profiles.api.ProfilesDatabaseType;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType;
import org.nypl.simplified.taskrecorder.api.TaskResult;

import java.net.URI;
import java.util.SortedMap;

import static org.nypl.simplified.accounts.api.AccountLoginState.*;

/**
 * The profiles controller.
 */

public interface ProfilesControllerType {

  /**
   * @return A read-only view of the current profiles
   */

  SortedMap<ProfileID, ProfileReadableType> profiles();

  /**
   * @return {@link ProfilesDatabaseType.AnonymousProfileEnabled#ANONYMOUS_PROFILE_ENABLED} if the anonymous profile is enabled
   */

  ProfilesDatabaseType.AnonymousProfileEnabled profileAnonymousEnabled();

  /**
   * @return The most recently selected profile, or the anonymous profile if it is enabled
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  ProfileReadableType profileCurrent()
    throws ProfileNoneCurrentException;

  /**
   * @return {@code true} if an account has been selected
   */

  boolean profileAnyIsCurrent();

  /**
   * @return An observable that publishes profile events
   */

  ObservableReadableType<ProfileEvent> profileEvents();

  /**
   * Create a profile, asynchronously, and return a profile event.
   *
   * @param account_provider The account provider used to create the default account
   * @param display_name     The profile display name
   * @param gender           The gender for the profile
   * @param date             The date of birth for the profile
   * @return A future that returns a status value
   */

  FluentFuture<ProfileCreationEvent> profileCreate(
    AccountProviderType account_provider,
    String display_name,
    String gender,
    LocalDate date);

  /**
   * Set the given profile as the current profile. The operation always succeeds if a profile
   * exists with the given ID.
   *
   * @param id The profile ID
   * @return A future that returns unit
   */

  FluentFuture<Unit> profileSelect(
    ProfileID id);

  /**
   * @return The current account in most recently selected profile, or the anonymous profile if it is enabled
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  AccountType profileAccountCurrent()
    throws ProfileNoneCurrentException;

  /**
   * Attempt to login using the given account of the current profile. The login is attempted
   * using the given credentials.
   *
   * @param account     The account ID
   * @param credentials The credentials
   * @return A future that returns the result of logging in
   */

  FluentFuture<TaskResult<AccountLoginErrorData, kotlin.Unit>> profileAccountLogin(
    AccountID account,
    AccountAuthenticationCredentials credentials);

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account already exists using the given provider.
   *
   * @param provider The account provider ID
   * @return A future that returns the result of creating the account
   */

  FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> profileAccountCreate(
    URI provider);

  /**
   * Create an account using the given account provider, or return an existing account
   * with that provider.
   *
   * @param provider The account provider ID
   * @return A future that returns the result of creating the account
   */

  FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> profileAccountCreateOrReturnExisting(
    URI provider);

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account does not exist using the given provider, or if deleting the account would result
   * in there being no accounts left.
   *
   * @param provider The account provider ID
   * @return A future that returns details of the task execution
   */

  FluentFuture<TaskResult<AccountDeleteErrorDetails, kotlin.Unit>> profileAccountDeleteByProvider(
    URI provider);

  /**
   * Switch the current account of the current profile to the one created by the given provider.
   *
   * @param provider The account provider ID
   */

  FluentFuture<ProfileAccountSelectEvent> profileAccountSelectByProvider(
    URI provider);

  /**
   * Find an account int the current profile using the given provider.
   *
   * @param provider The account provider ID
   * @throws ProfileNoneCurrentException          If the anonymous profile is disabled and no profile has been selected
   * @throws AccountsDatabaseNonexistentException If no account exists with the given provider
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  AccountType profileAccountFindByProvider(
    URI provider)
    throws ProfileNoneCurrentException, AccountsDatabaseNonexistentException;

  /**
   * @return An observable that publishes account events
   */

  ObservableReadableType<AccountEvent> accountEvents();

  /**
   * @return A list of all of the account providers used by the current profile
   * @throws ProfileNoneCurrentException                If the anonymous profile is disabled and no profile has been selected
   * @throws ProfileNonexistentAccountProviderException If the current account refers to an account provider that is not in the current set of known account providers
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  ImmutableList<AccountProviderType> profileCurrentlyUsedAccountProviders()
    throws ProfileNoneCurrentException, ProfileNonexistentAccountProviderException;

  /**
   * Attempt to log out of the given account of the current profile.
   *
   * @return A future that returns the result of logging out
   */

  FluentFuture<TaskResult<AccountLogoutErrorData, kotlin.Unit>>
  profileAccountLogout(AccountID account);

  /**
   * Update preferences for the current profile.
   *
   * @param preferences The new preferences
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  FluentFuture<Unit> profilePreferencesUpdate(
    ProfilePreferences preferences)
    throws ProfileNoneCurrentException;

  /**
   * Produce a feed of all the books in the current profile.
   *
   * @param request The feed request
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  FluentFuture<Feed.FeedWithoutGroups> profileFeed(
    ProfileFeedRequest request)
    throws ProfileNoneCurrentException;

  /**
   * Return the account that owns the given book ID in the current profile, or assume that the
   * current account owns the book.
   *
   * @param id The book ID
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  AccountType profileAccountForBook(BookID id)
    throws ProfileNoneCurrentException, AccountsDatabaseNonexistentException;

  /**
   * @return The global profile idle timer
   */

  ProfileIdleTimerType profileIdleTimer();
}
