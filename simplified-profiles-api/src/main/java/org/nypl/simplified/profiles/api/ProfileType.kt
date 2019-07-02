package org.nypl.simplified.profiles.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType

import java.io.IOException
import java.net.URI

/**
 * The readable and writable interface exposed by profiles.
 *
 * A profile aggregates a display name, a set of accounts, a set of preferences, and a current
 * account. Profiles are assigned monotonically increasing identifiers by the application, but the
 * identifiers themselves carry no meaning. It is an error to depend on the values of identifiers
 * for any kind of program logic. Exactly one account may be current at any given time. It is the
 * responsibility of the application to pick an account provider to be used as the default to
 * derive accounts for newly created profiles.
 *
 * Values of type `ProfileType` are required to be safe to read and write from multiple
 * threads concurrently.
 */

interface ProfileType : ProfileReadableType {

  /**
   * @return The accounts database for the profile
   */

  fun accountsDatabase(): AccountsDatabaseType

  /**
   * Set the profile's preferences to the given value.
   *
   * @param preferences The new preferences
   */

  @Throws(IOException::class)
  fun preferencesUpdate(preferences: ProfilePreferences)

  /**
   * Create an account using the given provider.
   *
   * @param accountProvider The account provider
   */

  @Throws(AccountsDatabaseException::class)
  fun createAccount(accountProvider: AccountProviderType): AccountType

  /**
   * Delete the account using the given provider.
   *
   * @param accountProvider The account provider
   * @return The ID of the deleted account
   * @throws AccountsDatabaseException On accounts database problems
   * @see AccountsDatabaseType.deleteAccountByProvider
   */

  @Throws(AccountsDatabaseException::class)
  fun deleteAccountByProvider(accountProvider: URI): AccountID

  /**
   * Set the account created by the given provider to be the current account in the profile.
   *
   * @param accountProvider The account provider
   */

  @Throws(AccountsDatabaseNonexistentException::class)
  fun selectAccount(accountProvider: URI): AccountType
}
