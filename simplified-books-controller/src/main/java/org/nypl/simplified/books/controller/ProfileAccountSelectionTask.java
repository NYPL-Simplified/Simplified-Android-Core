package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.api.AccountProviderCollectionType;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.observable.ObservableType;
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent;
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectFailed;
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectFailed.ErrorCode;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfileType;
import org.nypl.simplified.profiles.api.ProfilesDatabaseType;

import java.net.URI;
import java.util.concurrent.Callable;

final class ProfileAccountSelectionTask implements Callable<ProfileAccountSelectEvent> {

  private final ProfilesDatabaseType profiles;
  private final ObservableType<ProfileEvent> profile_events;
  private final FunctionType<Unit, AccountProviderCollectionType> account_providers;
  private final URI provider_id;

  public ProfileAccountSelectionTask(
      final ProfilesDatabaseType profiles,
      final ObservableType<ProfileEvent> profile_events,
      final FunctionType<Unit, AccountProviderCollectionType> account_providers,
      final URI provider) {

    this.profiles =
        NullCheck.notNull(profiles, "profiles");
    this.profile_events =
        NullCheck.notNull(profile_events, "profile_events");
    this.account_providers =
        NullCheck.notNull(account_providers, "account_providers");
    this.provider_id =
        NullCheck.notNull(provider, "provider_id");
  }

  private ProfileAccountSelectEvent run() {
    try {
      final ProfileType profile = this.profiles.currentProfileUnsafe();
      final AccountID id_then = profile.accountCurrent().id();
      final AccountProviderCollectionType providers = this.account_providers.call(Unit.unit());
      final AccountProviderType provider = providers.providers().get(provider_id);
      if (provider != null) {
        profile.selectAccount(provider);
        return ProfileAccountSelectEvent.ProfileAccountSelectSucceeded.of(id_then, profile.accountCurrent().id());
      }

      return ProfileAccountSelectFailed.of(ErrorCode.ERROR_ACCOUNT_PROVIDER_UNKNOWN, Option.none());
    } catch (final ProfileNoneCurrentException e) {
      return ProfileAccountSelectFailed.of(ErrorCode.ERROR_PROFILE_NONE_CURRENT, Option.some(e));
    } catch (final AccountsDatabaseNonexistentException e) {
      return ProfileAccountSelectFailed.of(ErrorCode.ERROR_ACCOUNT_NONEXISTENT, Option.some(e));
    }
  }

  @Override
  public ProfileAccountSelectEvent call() throws Exception {
    final ProfileAccountSelectEvent event = run();
    this.profile_events.send(event);
    return event;
  }
}
