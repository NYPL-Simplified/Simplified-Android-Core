package org.nypl.simplified.books.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.joda.time.LocalDate;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventCreation;
import org.nypl.simplified.books.accounts.AccountEventDeletion;
import org.nypl.simplified.books.accounts.AccountEventLogin;
import org.nypl.simplified.books.accounts.AccountEventLogout;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.books.idle_timer.ProfileIdleTimerType;
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent;
import org.nypl.simplified.books.profiles.ProfileCreationEvent;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileID;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.books.profiles.ProfilePreferences;
import org.nypl.simplified.books.profiles.ProfileReadableType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.observable.ObservableReadableType;

import java.net.URI;
import java.util.SortedMap;

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

  ListenableFuture<ProfileCreationEvent> profileCreate(
      AccountProvider account_provider,
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

  ListenableFuture<Unit> profileSelect(
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
   * Attempt to login using the current account of the current profile. The login is attempted
   * using the given credentials.
   *
   * @param credentials The credentials
   * @return A future that returns a login event
   */

  ListenableFuture<AccountEventLogin> profileAccountCurrentLogin(
      AccountAuthenticationCredentials credentials);

  /**
   * Attempt to login using the given account of the current profile. The login is attempted
   * using the given credentials.
   *
   * @param account     The account ID
   * @param credentials The credentials
   * @return A future that returns a login event
   */

  ListenableFuture<AccountEventLogin> profileAccountLogin(
      AccountID account,
      AccountAuthenticationCredentials credentials);

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account already exists using the given provider.
   *
   * @param provider The account provider ID
   * @return A future that returns a login event
   */

  ListenableFuture<AccountEventCreation> profileAccountCreate(
      URI provider);

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account does not exist using the given provider, or if deleting the account would result
   * in there being no accounts left.
   *
   * @param provider The account provider ID
   * @return A future that returns a login event
   */

  ListenableFuture<AccountEventDeletion> profileAccountDeleteByProvider(
      URI provider);

  /**
   * Switch the current account of the current profile to the one created by the given provider.
   *
   * @param provider The account provider ID
   */

  ListenableFuture<ProfileAccountSelectEvent> profileAccountSelectByProvider(
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

  ImmutableList<AccountProvider> profileCurrentlyUsedAccountProviders()
      throws ProfileNoneCurrentException, ProfileNonexistentAccountProviderException;

  /**
   * Attempt to log out using the current account of the current profile.
   *
   * @return A future that returns a logout event
   */

  ListenableFuture<AccountEventLogout> profileAccountLogout();

  /**
   * Determine the root URI of the catalog based on the current account of the current profile,
   * and any age-related settings in the current profile. For example, some accounts require different
   * root catalog URIs based on whether or not the reader is over 13.
   *
   * @return The calculated catalog root URI
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  URI profileAccountCurrentCatalogRootURI() throws ProfileNoneCurrentException;

  /**
   * Set a bookmark.
   *
   * @param book_id      The book ID
   * @param new_location The book location
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  ListenableFuture<Unit> profileBookmarkSet(
      BookID book_id,
      ReaderBookLocation new_location)
      throws ProfileNoneCurrentException;

  /**
   * Retrieve the last bookmark for a given book.
   *
   * @param book_id The book ID
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  OptionType<ReaderBookLocation> profileBookmarkGet(
      BookID book_id)
      throws ProfileNoneCurrentException;

  /**
   * Update preferences for the current profile.
   *
   * @param preferences The new preferences
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   */

  ListenableFuture<Unit> profilePreferencesUpdate(
      ProfilePreferences preferences)
      throws ProfileNoneCurrentException;

  /**
   * Produce a feed of all the books in the current profile.
   *
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see #profileSelect(ProfileID)
   * @see #profileAnonymousEnabled()
   * @param request The feed request
   */

  ListenableFuture<FeedWithoutGroups> profileFeed(
      ProfileFeedRequest request)
      throws ProfileNoneCurrentException;

  /**
   * Return the account that owns the given book ID in the current profile, or assume that the
   * current account owns the book.
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
