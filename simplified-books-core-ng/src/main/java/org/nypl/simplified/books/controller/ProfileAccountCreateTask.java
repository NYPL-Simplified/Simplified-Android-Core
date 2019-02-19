package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventCreation;
import org.nypl.simplified.books.accounts.AccountEventCreation.AccountCreationFailed;
import org.nypl.simplified.books.accounts.AccountEventCreation.AccountCreationSucceeded;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseException;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.observable.ObservableType;

import java.net.URI;
import java.util.concurrent.Callable;

final class ProfileAccountCreateTask implements Callable<AccountEventCreation> {

  private final ProfilesDatabaseType profiles;
  private final FunctionType<Unit, AccountProviderCollection> account_providers;
  private final URI provider_id;
  private final ObservableType<AccountEvent> account_events;

  ProfileAccountCreateTask(
    final ProfilesDatabaseType profiles,
    final ObservableType<AccountEvent> account_events,
    final FunctionType<Unit, AccountProviderCollection> account_providers,
    final URI provider) {

    this.profiles =
      NullCheck.notNull(profiles, "Profiles");
    this.account_events =
      NullCheck.notNull(account_events, "Account events");
    this.account_providers =
      NullCheck.notNull(account_providers, "Account providers");
    this.provider_id =
      NullCheck.notNull(provider, "Provider");
  }

  private AccountEventCreation execute() {
    try {
      final AccountProviderCollection providers_now = this.account_providers.call(Unit.unit());
      final AccountProvider provider = providers_now.providers().get(this.provider_id);

      if (provider != null) {
        final ProfileType profile = this.profiles.currentProfileUnsafe();
        final AccountType account = profile.createAccount(provider);
        return AccountCreationSucceeded.of(account.id(), provider);
      }

      throw new ProfileNonexistentAccountProviderException("Unrecognized provider: " + this.provider_id);
    } catch (final ProfileNoneCurrentException | ProfileNonexistentAccountProviderException | AccountsDatabaseException e) {
      return AccountCreationFailed.of(e);
    }
  }

  @Override
  public AccountEventCreation call() {
    final AccountEventCreation event = execute();
    this.account_events.send(event);
    return event;
  }
}
