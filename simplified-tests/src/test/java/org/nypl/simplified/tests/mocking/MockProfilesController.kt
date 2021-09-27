package org.nypl.simplified.tests.mocking

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.net.URI
import java.util.SortedMap
import java.util.UUID

class MockProfilesController(
  profileCount: Int,
  accountCount: Int
) : ProfilesControllerType {

  override fun profileCreate(
    displayName: String,
    accountProvider: AccountProviderType,
    descriptionUpdate: (ProfileDescription) -> ProfileDescription
  ): FluentFuture<ProfileCreationEvent> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileUpdate(update: (ProfileDescription) -> ProfileDescription): FluentFuture<ProfileUpdated> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileUpdateFor(profile: ProfileID, update: (ProfileDescription) -> ProfileDescription): FluentFuture<ProfileUpdated> {
    return FluentFuture.from(SettableFuture.create())
  }

  val profileList: List<MockProfile> =
    IntRange(1, profileCount)
      .toList()
      .map {
        MockProfile(ProfileID(UUID.randomUUID()), accountCount)
      }

  val profiles: SortedMap<ProfileID, MockProfile> =
    this.profileList.map { profile -> Pair(profile.id, profile) }
      .toMap()
      .toSortedMap()

  val profileEventSource: PublishSubject<ProfileEvent> =
    PublishSubject.create<ProfileEvent>()
  val accountEventSource: PublishSubject<AccountEvent> =
    PublishSubject.create<AccountEvent>()

  override fun profiles(): SortedMap<ProfileID, ProfileReadableType> {
    return this.profiles as SortedMap<ProfileID, ProfileReadableType>
  }

  override fun profileAnonymousEnabled(): ProfilesDatabaseType.AnonymousProfileEnabled {
    return ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
  }

  override fun profileCurrent(): ProfileReadableType {
    return this.profileList[0]
  }

  override fun profileAnyIsCurrent(): Boolean {
    return true
  }

  override fun profileEvents(): Observable<ProfileEvent> {
    return this.profileEventSource
  }

  override fun profileSelect(profileID: ProfileID): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  data class ProfileAccountLogin(
    val account: AccountID,
    val credentials: AccountAuthenticationCredentials
  )

  var profileAccountLogins =
    mutableListOf<ProfileAccountLoginRequest>()

  override fun profileAccountLogin(
    request: ProfileAccountLoginRequest
  ): FluentFuture<TaskResult<Unit>> {
    this.profileAccountLogins.add(request)
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreate(provider: URI): FluentFuture<TaskResult<AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreateCustomOPDS(opdsFeed: URI): FluentFuture<TaskResult<AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreateOrReturnExisting(provider: URI): FluentFuture<TaskResult<AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountDeleteByProvider(provider: URI): FluentFuture<TaskResult<Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountFindByProvider(provider: URI): AccountType {
    return this.profileCurrent().accountsByProvider()[provider]!!
  }

  override fun accountEvents(): Observable<AccountEvent> {
    return this.accountEventSource
  }

  override fun profileCurrentlyUsedAccountProviders(): ImmutableList<AccountProviderType> {
    return ImmutableList.of()
  }

  val profileAccountLogouts = mutableListOf<AccountID>()

  override fun profileAccountLogout(accountID: AccountID): FluentFuture<TaskResult<Unit>> {
    this.profileAccountLogouts.add(accountID)
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileFeed(request: ProfileFeedRequest): FluentFuture<Feed.FeedWithoutGroups> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountForBook(bookID: BookID): AccountType {
    return TODO()
  }

  override fun profileDelete(profileID: ProfileID): FluentFuture<ProfileDeletionEvent> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileIdleTimer(): ProfileIdleTimerType {
    TODO()
  }
}
