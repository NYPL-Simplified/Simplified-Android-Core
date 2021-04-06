package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountID

class FakeAccountCredentialStorage : AccountAuthenticationCredentialsStoreType {

  private val store = mutableMapOf<AccountID, AccountAuthenticationCredentials>()

  override fun get(account: AccountID): AccountAuthenticationCredentials? =
    this.store[account]

  override fun size(): Int =
    this.store.size

  override fun put(account: AccountID, credentials: AccountAuthenticationCredentials) {
    this.store[account] = credentials
  }

  override fun delete(account: AccountID) {
    this.store.remove(account)
  }
}
