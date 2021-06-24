package org.nypl.simplified.profiles.controller.api

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.Observable
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.net.URI
import java.util.SortedMap

/**
 * The profiles controller.
 */

interface ProfilesControllerType {

  /**
   * @return A read-only view of the current profiles
   */

  fun profiles(): SortedMap<ProfileID, ProfileReadableType>

  /**
   * @return The profile, or null.
   */

  fun profile(id: ProfileID) = this.profiles()[id]

  /**
   * @return [ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED] if the anonymous profile is enabled
   */

  fun profileAnonymousEnabled(): ProfilesDatabaseType.AnonymousProfileEnabled

  /**
   * @return The most recently selected profile, or the anonymous profile if it is enabled
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class)
  fun profileCurrent(): ProfileReadableType

  /**
   * @return `true` if an account has been selected
   */

  fun profileAnyIsCurrent(): Boolean

  /**
   * @return An observable that publishes profile events
   */

  fun profileEvents(): Observable<ProfileEvent>

  /**
   * Create a profile, asynchronously, and return a profile event.
   *
   * @param accountProvider The account provider used to create the default account
   * @return A future that returns a status value
   */

  fun profileCreate(
    displayName: String,
    accountProvider: AccountProviderType,
    descriptionUpdate: (ProfileDescription) -> ProfileDescription = { it }
  ): FluentFuture<ProfileCreationEvent>

  /**
   * Delete a profile, asynchronously, and return a profile event.
   *
   * @param profileID The ID of the profile to delete
   *
   * @return A future that returns a status value
   */

  fun profileDelete(
    profileID: ProfileID
  ): FluentFuture<ProfileDeletionEvent>

  /**
   * Set the given profile as the current profile. The operation always succeeds if a profile
   * exists with the given ID.
   *
   * @param profileID The profile ID
   * @return A future that returns unit
   */

  fun profileSelect(
    profileID: ProfileID
  ): FluentFuture<Unit>

  /**
   * Attempt to login using the given account of the current profile. The login is attempted
   * using the given credentials.
   *
   * @param request The login request
   * @return A future that returns the result of logging in
   */

  fun profileAccountLogin(
    request: ProfileAccountLoginRequest
  ): FluentFuture<TaskResult<Unit>>

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account already exists using the given provider.
   *
   * @param provider The account provider ID
   * @return A future that returns the result of creating the account
   */

  fun profileAccountCreate(
    provider: URI
  ): FluentFuture<TaskResult<AccountType>>

  /**
   * Create an account using the URI of an OPDS feed. The given feed is parsed in order to
   * infer enough information to create an account provider description.
   *
   * @param opdsFeed The OPDS feed URI
   * @return A future that returns the result of creating the account
   */

  fun profileAccountCreateCustomOPDS(
    opdsFeed: URI
  ): FluentFuture<TaskResult<AccountType>>

  /**
   * Create an account using the given account provider, or return an existing account
   * with that provider.
   *
   * @param provider The account provider ID
   * @return A future that returns the result of creating the account
   */

  fun profileAccountCreateOrReturnExisting(
    provider: URI
  ): FluentFuture<TaskResult<AccountType>>

  /**
   * Create an account using the given account provider. The operation will fail if
   * an account does not exist using the given provider, or if deleting the account would result
   * in there being no accounts left.
   *
   * @param provider The account provider ID
   * @return A future that returns details of the task execution
   */

  fun profileAccountDeleteByProvider(
    provider: URI
  ): FluentFuture<TaskResult<Unit>>

  /**
   * Find an account int the current profile using the given provider.
   *
   * @param provider The account provider ID
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @throws AccountsDatabaseNonexistentException If no account exists with the given provider
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  fun profileAccountFindByProvider(
    provider: URI
  ): AccountType

  /**
   * @return An observable that publishes account events
   */

  fun accountEvents(): Observable<AccountEvent>

  /**
   * @return A list of all of the account providers used by the current profile
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @throws ProfileNonexistentAccountProviderException If the current account refers to an account provider that is not in the current set of known account providers
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class, ProfileNonexistentAccountProviderException::class)
  fun profileCurrentlyUsedAccountProviders(): ImmutableList<AccountProviderType>

  /**
   * Attempt to log out of the given account of the current profile.
   *
   * @return A future that returns the result of logging out
   */

  fun profileAccountLogout(
    accountID: AccountID
  ): FluentFuture<TaskResult<Unit>>

  /**
   * Update values for the current profile.
   *
   * @param update A function that transforms the profile's current description
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class)
  fun profileUpdate(
    update: (ProfileDescription) -> ProfileDescription
  ): FluentFuture<ProfileUpdated>

  /**
   * Update values for the given profile.
   *
   * @param profile The target profile
   * @param update A function that transforms the profile's current description
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  fun profileUpdateFor(
    profile: ProfileID,
    update: (ProfileDescription) -> ProfileDescription
  ): FluentFuture<ProfileUpdated>

  /**
   * Produce a feed of all the books in the current profile.
   *
   * @param request The feed request
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class)
  fun profileFeed(
    request: ProfileFeedRequest
  ): FluentFuture<Feed.FeedWithoutGroups>

  /**
   * Return the account that owns the given book ID in the current profile, or assume that the
   * current account owns the book.
   *
   * @param bookID The book ID
   * @throws ProfileNoneCurrentException If the anonymous profile is disabled and no profile has been selected
   * @see .profileSelect
   * @see .profileAnonymousEnabled
   */

  @Throws(ProfileNoneCurrentException::class, AccountsDatabaseNonexistentException::class)
  fun profileAccountForBook(bookID: BookID): AccountType

  /**
   * @return The global profile idle timer
   */

  fun profileIdleTimer(): ProfileIdleTimerType
}
