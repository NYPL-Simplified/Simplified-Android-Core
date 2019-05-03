package org.nypl.simplified.tests.android.splash

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Unit
import org.joda.time.LocalDate
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventCreation
import org.nypl.simplified.books.accounts.AccountEventDeletion
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountProvider
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.controller.ProfileFeedRequest
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.feeds.Feed
import org.nypl.simplified.books.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent
import org.nypl.simplified.books.profiles.ProfileCreationEvent
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfileID
import org.nypl.simplified.books.profiles.ProfilePreferences
import org.nypl.simplified.books.profiles.ProfileReadableType
import org.nypl.simplified.books.profiles.ProfilesDatabaseType
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.observable.ObservableReadableType
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

  override fun profileCreate(account_provider: AccountProvider?, display_name: String?, gender: String?, date: LocalDate?): FluentFuture<ProfileCreationEvent> {
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

  override fun profileCurrentlyUsedAccountProviders(): ImmutableList<AccountProvider> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountLogout(account: AccountID?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileAccountCurrentCatalogRootURI(): URI {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileBookmarkSet(book_id: BookID?, new_location: ReaderBookLocation?): FluentFuture<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun profileBookmarkGet(book_id: BookID?): OptionType<ReaderBookLocation> {
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