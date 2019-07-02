package org.nypl.simplified.tests.local.books.accounts

import android.content.Context
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistry
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryType
import org.nypl.simplified.accounts.source.api.AccountProviderSourceType
import org.nypl.simplified.tests.books.accounts.AccountProviderDescriptionRegistryContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountProviderDescriptionRegistryTest : AccountProviderDescriptionRegistryContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AccountProviderDescriptionRegistryTest::class.java)

  override val context: Context
    get() = Mockito.mock(Context::class.java)

  override fun createRegistry(
    defaultProvider: AccountProviderType,
    sources: List<AccountProviderSourceType>): AccountProviderDescriptionRegistryType {
    return AccountProviderDescriptionRegistry.createFrom(this.context, sources, defaultProvider)
  }

}
