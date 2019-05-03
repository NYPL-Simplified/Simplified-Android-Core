package org.nypl.simplified.profiles.api;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;

import java.io.File;
import java.net.URI;
import java.util.SortedMap;

/**
 * <p>The readable interface exposed by profiles.</p>
 * <p>A profile aggregates a display name, a set of accounts, a set of preferences, and a current
 * account. Profiles are assigned monotonically increasing identifiers by the application, but the
 * identifiers themselves carry no meaning. It is an error to depend on the values of identifiers
 * for any kind of program logic. Exactly one account may be current at any given time. It is the
 * responsibility of the application to pick an account provider to be used as the default to derive
 * accounts for newly created profiles.</p>
 */

public interface ProfileReadableType extends Comparable<ProfileReadableType> {

  /**
   * @return The unique profile identifier
   */

  ProfileID id();

  /**
   * @return {@code true} iff this profile is the anonymous profile
   */

  boolean isAnonymous();

  /**
   * @return The directory containing the profile's data
   */

  File directory();

  /**
   * @return The profile's display name
   */

  String displayName();

  /**
   * @return {@code true} Iff this profile is the current profile
   */

  boolean isCurrent();

  /**
   * @return The current account
   */

  AccountType accountCurrent();

  /**
   * @return A read-only map of the accounts for this profile
   */

  SortedMap<AccountID, AccountType> accounts();

  /**
   * @return Access to the profile's preferences
   */

  ProfilePreferences preferences();

  /**
   * @return A read-only map of the accounts for this profile, organized by provider
   */

  SortedMap<URI, AccountType> accountsByProvider();

  /**
   * @param account_id The account ID
   * @return The account with the given ID
   */

  AccountType account(AccountID account_id)
    throws AccountsDatabaseNonexistentException;
}
