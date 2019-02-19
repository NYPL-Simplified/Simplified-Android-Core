package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventDeletion;
import org.nypl.simplified.books.accounts.AccountEventDeletion.AccountDeletionFailed;
import org.nypl.simplified.books.accounts.AccountEventDeletion.AccountDeletionSucceeded;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseLastAccountException;
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent.ProfileAccountSelectSucceeded;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.observable.ObservableType;

import java.net.URI;
import java.util.concurrent.Callable;

import static org.nypl.simplified.books.accounts.AccountEventDeletion.AccountDeletionFailed.ErrorCode.ERROR_ACCOUNT_ONLY_ONE_REMAINING;
import static org.nypl.simplified.books.accounts.AccountEventDeletion.AccountDeletionFailed.ErrorCode.ERROR_GENERAL;

final class ProfileAccountDeleteTask implements Callable<AccountEventDeletion> {

  private final ProfilesDatabaseType profiles;
  private final FunctionType<Unit, AccountProviderCollection> account_providers;
  private final URI provider_id;
  private final ObservableType<ProfileEvent> profile_events;
  private final ObservableType<AccountEvent> account_events;

  ProfileAccountDeleteTask(
    final ProfilesDatabaseType profiles,
    final ObservableType<AccountEvent> account_events,
    final ObservableType<ProfileEvent> profile_events,
    final FunctionType<Unit, AccountProviderCollection> account_providers,
    final URI provider) {

    this.profiles =
      NullCheck.notNull(profiles, "Profiles");
    this.account_events =
      NullCheck.notNull(account_events, "Account events");
    this.profile_events =
      NullCheck.notNull(profile_events, "Profile events");
    this.account_providers =
      NullCheck.notNull(account_providers, "Account providers");
    this.provider_id =
      NullCheck.notNull(provider, "Provider");
  }

  protected AccountEventDeletion execute() {

    try {
      final AccountProviderCollection providers_now = this.account_providers.call(Unit.unit());
      final AccountProvider provider = providers_now.providers().get(this.provider_id);

      if (provider != null) {
        final ProfileType profile = this.profiles.currentProfileUnsafe();
        final AccountType account_then = profile.accountCurrent();
        profile.deleteAccountByProvider(provider);
        final AccountType account_now = profile.accountCurrent();
        if (!account_now.id().equals(account_then.id())) {
          this.profile_events.send(
            ProfileAccountSelectSucceeded.of(account_then.id(), account_now.id()));
        }
        return AccountDeletionSucceeded.of(account_now.id(), provider);
      }

      throw new ProfileNonexistentAccountProviderException("Unrecognized provider: " + this.provider_id);
    } catch (final AccountsDatabaseLastAccountException e) {
      return AccountDeletionFailed.of(ERROR_ACCOUNT_ONLY_ONE_REMAINING, Option.some(e));
    } catch (final Exception e) {
      return AccountDeletionFailed.of(ERROR_GENERAL, Option.some(e));
    }
  }

  @Override
  public AccountEventDeletion call() throws Exception {
    final AccountEventDeletion event = execute();
    this.account_events.send(event);
    return event;
  }
}
