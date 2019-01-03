package org.nypl.simplified.books.profiles;

import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseException;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.accounts.AccountsDatabaseType;

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
      AccountProvider account_provider)
      throws AccountsDatabaseException;

  /**
   * Delete the account using the given provider.
   *
   * @param account_provider The account provider
   * @return The ID of the deleted account
   * @throws AccountsDatabaseException On accounts database problems
   * @see AccountsDatabaseType#deleteAccountByProvider(AccountProvider)
   */

  AccountID deleteAccountByProvider(
      AccountProvider account_provider)
      throws AccountsDatabaseException;

  /**
   * Set the account created by the given provider to be the current account in the profile.
   *
   * @param account_provider The account provider
   */

  AccountType selectAccount(
      AccountProvider account_provider)
      throws AccountsDatabaseNonexistentException;
}
