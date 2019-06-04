package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.api.AccountEvent;
import org.nypl.simplified.accounts.api.AccountEventCreation;
import org.nypl.simplified.accounts.api.AccountProviderCollectionType;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException;
import org.nypl.simplified.observable.ObservableType;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.profiles.api.ProfileType;
import org.nypl.simplified.profiles.api.ProfilesDatabaseType;

import java.net.URI;
import java.util.concurrent.Callable;

final class ProfileAccountCreateTask implements Callable<AccountEventCreation> {

  private final ProfilesDatabaseType profiles;
  private final FunctionType<Unit, AccountProviderCollectionType> account_providers;
  private final URI provider_id;
  private final ObservableType<AccountEvent> account_events;

  ProfileAccountCreateTask(
    final ProfilesDatabaseType profiles,
    final ObservableType<AccountEvent> account_events,
    final FunctionType<Unit, AccountProviderCollectionType> account_providers,
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
      final AccountProviderCollectionType providers_now = this.account_providers.call(Unit.unit());
      final AccountProviderType provider = providers_now.providers().get(this.provider_id);

      if (provider != null) {
        final ProfileType profile = this.profiles.currentProfileUnsafe();
        final AccountType account = profile.createAccount(provider);
        return AccountEventCreation.AccountCreationSucceeded.of(account.id(), provider);
      }

      throw new ProfileNonexistentAccountProviderException("Unrecognized provider: " + this.provider_id);
    } catch (final ProfileNoneCurrentException | ProfileNonexistentAccountProviderException | AccountsDatabaseException e) {
      return AccountEventCreation.AccountCreationFailed.of(e);
    }
  }

  @Override
  public AccountEventCreation call() {
    final AccountEventCreation event = execute();
    this.account_events.send(event);
    return event;
  }
}
