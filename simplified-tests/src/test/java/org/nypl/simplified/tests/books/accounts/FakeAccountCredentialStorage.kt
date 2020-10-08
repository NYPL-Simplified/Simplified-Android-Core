package org.nypl.simplified.tests.books.accounts

class FakeAccountCredentialStorage : org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType {

  private val store = mutableMapOf<org.nypl.simplified.accounts.api.AccountID, org.nypl.simplified.accounts.api.AccountAuthenticationCredentials>()

  override fun get(account: org.nypl.simplified.accounts.api.AccountID): org.nypl.simplified.accounts.api.AccountAuthenticationCredentials? =
    this.store[account]

  override fun size(): Int =
    this.store.size

  override fun put(account: org.nypl.simplified.accounts.api.AccountID, credentials: org.nypl.simplified.accounts.api.AccountAuthenticationCredentials) {
    this.store[account] = credentials
  }

  override fun delete(account: org.nypl.simplified.accounts.api.AccountID) {
    this.store.remove(account)
  }
}
