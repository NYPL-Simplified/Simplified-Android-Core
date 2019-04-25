package org.nypl.simplified.tests.books.accounts

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.books.accounts.AccountID

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