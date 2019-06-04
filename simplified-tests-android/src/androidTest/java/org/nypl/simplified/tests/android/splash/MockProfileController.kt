package org.nypl.simplified.tests.android.splash

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.Unit
import org.joda.time.LocalDate
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.observable.ObservableReadableType
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
import java.net.URI
import java.util.SortedMap

class MockProfileController : ProfilesControllerType {

  override fun profileAnyIsCurrent(): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  var anonymous = ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED

  override fun profiles(): SortedMap<ProfileID, ProfileReadableType> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAnonymousEnabled(): ProfilesDatabaseType.AnonymousProfileEnabled {
    return this.anonymous
  }

  override fun profileCurrent(): ProfileReadableType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileEvents(): ObservableReadableType<ProfileEvent> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileCreate(account_provider: AccountProviderType?, display_name: String?, gender: String?, date: LocalDate?): FluentFuture<ProfileCreationEvent> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileSelect(id: ProfileID?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountCurrent(): AccountType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountLogin(account: AccountID?, credentials: AccountAuthenticationCredentials?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountCreate(provider: URI?): FluentFuture<AccountEventCreation> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountDeleteByProvider(provider: URI?): FluentFuture<AccountEventDeletion> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountSelectByProvider(provider: URI?): FluentFuture<ProfileAccountSelectEvent> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountFindByProvider(provider: URI?): AccountType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun accountEvents(): ObservableReadableType<AccountEvent> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileCurrentlyUsedAccountProviders(): ImmutableList<AccountProviderType> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountLogout(account: AccountID?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profilePreferencesUpdate(preferences: ProfilePreferences?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileFeed(request: ProfileFeedRequest?): FluentFuture<Feed.FeedWithoutGroups> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountForBook(id: BookID?): AccountType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileIdleTimer(): ProfileIdleTimerType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}