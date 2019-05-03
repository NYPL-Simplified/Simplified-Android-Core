package org.nypl.simplified.accounts.api

/**
 * A generic interface for safely storing credentials for accounts.
 */

interface AccountAuthenticationCredentialsStoreType {

  /**
   * Obtain the credentials for the given account, if any.
   */

  fun get(account: AccountID): AccountAuthenticationCredentials?

  /**
   * Store the credentials for the given account, replacing any existing credentials.
   */

  fun put(
    account: AccountID,
    credentials: AccountAuthenticationCredentials)

  /**
   * Delete the credentials for the given account, if any.
   */

  fun delete(
    account: AccountID)

  /**
   * The number of entries in the store
   */

  fun size(): Int
}