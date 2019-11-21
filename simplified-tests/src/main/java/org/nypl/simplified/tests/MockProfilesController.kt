package org.nypl.simplified.tests

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.LocalDate
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountDeleteErrorDetails
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.net.URI
import java.util.SortedMap
import java.util.UUID

object MockProfilesController : ProfilesControllerType {

  private val profileList =
    listOf(
      MockProfile(ProfileID(UUID.randomUUID())),
      MockProfile(ProfileID(UUID.randomUUID())),
      MockProfile(ProfileID(UUID.randomUUID())),
      MockProfile(ProfileID(UUID.randomUUID())),
      MockProfile(ProfileID(UUID.randomUUID())))

  private val profiles =
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
    return this.profileEventSource;
  }

  override fun profileCreate(
    account_provider: AccountProviderType?,
    display_name: String?,
    gender: String?,
    date: LocalDate?
  ): FluentFuture<ProfileCreationEvent> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileSelect(id: ProfileID): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCurrent(): AccountType {
    return this.profileList[0].accountCurrent()
  }

  data class ProfileAccountLogin(
    val account: AccountID,
    val credentials: AccountAuthenticationCredentials)

  var profileAccountLogins =
    mutableListOf<ProfileAccountLogin>()

  override fun profileAccountLogin(
    account: AccountID,
    credentials: AccountAuthenticationCredentials
  ): FluentFuture<TaskResult<AccountLoginState.AccountLoginErrorData, Unit>> {
    this.profileAccountLogins.add(ProfileAccountLogin(account, credentials))
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreate(provider: URI): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreateCustomOPDS(opdsFeed: URI): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountCreateOrReturnExisting(provider: URI): FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountDeleteByProvider(provider: URI): FluentFuture<TaskResult<AccountDeleteErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountSelectByProvider(provider: URI): FluentFuture<ProfileAccountSelectEvent> {
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

  override fun profileAccountLogout(account: AccountID): FluentFuture<TaskResult<AccountLoginState.AccountLogoutErrorData, Unit>> {
    this.profileAccountLogouts.add(account)
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profilePreferencesUpdate(preferences: ProfilePreferences): FluentFuture<com.io7m.jfunctional.Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileFeed(request: ProfileFeedRequest): FluentFuture<Feed.FeedWithoutGroups> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun profileAccountForBook(id: BookID): AccountType {
    return TODO()
  }

  override fun profileIdleTimer(): ProfileIdleTimerType {
    TODO()
  }
}