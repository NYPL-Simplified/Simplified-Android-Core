package org.nypl.simplified.tests

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileType
import java.io.File
import java.net.URI
import java.util.SortedMap
import java.util.UUID

class MockProfile(
  override val id: ProfileID,
  accountCount: Int) : ProfileType {

  private val accountList =
    IntRange(1, accountCount)
      .toList()
      .map { MockAccount(AccountID(UUID.randomUUID())) }
      .toMutableList()

  private val accounts =
    this.accountList.map { account -> Pair(account.id, account) }
      .toMap()
      .toSortedMap()

  override fun accountsDatabase(): AccountsDatabaseType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun preferencesUpdate(preferences: ProfilePreferences) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun createAccount(accountProvider: AccountProviderType): AccountType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun deleteAccountByProvider(accountProvider: URI): AccountID {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun selectAccount(accountProvider: URI): AccountType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override val isAnonymous: Boolean
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

  override val isCurrent: Boolean
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

  override val directory: File
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

  override val displayName: String
    get() = "Profile ${id.uuid}"

  override fun accountCurrent(): AccountType {
    return this.accountList[0]
  }

  override fun accounts(): SortedMap<AccountID, AccountType> {
    return this.accounts as SortedMap<AccountID, AccountType>
  }

  override fun preferences(): ProfilePreferences {
    return ProfilePreferences.builder()
      .setShowTestingLibraries(true)
      .build()
  }

  override fun accountsByProvider(): SortedMap<URI, AccountType> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun account(accountId: AccountID): AccountType {
    return this.accounts[accountId] ?: throw AccountsDatabaseNonexistentException("No such account!")
  }

  override fun compareTo(other: ProfileReadableType): Int {
    return this.id.compareTo(other.id)
  }
}