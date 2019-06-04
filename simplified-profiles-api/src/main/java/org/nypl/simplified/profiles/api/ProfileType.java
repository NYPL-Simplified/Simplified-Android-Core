package org.nypl.simplified.profiles.api;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.api.AccountProviderType;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType;

import java.io.IOException;

/**
 * <p>The readable and writable interface exposed by profiles.</p>
 * <p>A profile aggregates a display name, a set of accounts, a set of preferences, and a current
 * account. Profiles are assigned monotonically increasing identifiers by the application, but the
 * identifiers themselves carry no meaning. It is an error to depend on the values of identifiers
 * for any kind of program logic. Exactly one account may be current at any given time. It is the
 * responsibility of the application to pick an account provider to be used as the default to
 * derive accounts for newly created profiles.</p>
 * <p>Values of type {@code ProfileType} are required to be safe to read and write from multiple
 * threads concurrently.</p>
 */

public interface ProfileType extends ProfileReadableType {

  /**
   * @return The accounts database for the profile
   */

  AccountsDatabaseType accountsDatabase();

  /**
   * Set the profile's preferences to the given value.
   *
   * @param preferences The new preferences
   */

  void preferencesUpdate(
    ProfilePreferences preferences) throws IOException;

  /**
   * Create an account using the given provider.
   *
   * @param account_provider The account provider
   */

  AccountType createAccount(
    AccountProviderType account_provider)
    throws AccountsDatabaseException;

  /**
   * Delete the account using the given provider.
   *
   * @param account_provider The account provider
   * @return The ID of the deleted account
   * @throws AccountsDatabaseException On accounts database problems
   * @see AccountsDatabaseType#deleteAccountByProvider(AccountProviderType)
   */

  AccountID deleteAccountByProvider(
    AccountProviderType account_provider)
    throws AccountsDatabaseException;

  /**
   * Set the account created by the given provider to be the current account in the profile.
   *
   * @param account_provider The account provider
   */

  AccountType selectAccount(
    AccountProviderType account_provider)
    throws AccountsDatabaseNonexistentException;
}
