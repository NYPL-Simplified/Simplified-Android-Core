package org.nypl.simplified.tests.books.accounts

import android.content.Context
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.registry.AccountProviderRegistry
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountProviderDescriptionRegistryTest : AccountProviderDescriptionRegistryContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AccountProviderDescriptionRegistryTest::class.java)

  override val context: Context
    get() = Mockito.mock(Context::class.java)

  override fun createRegistry(
    defaultProvider: AccountProviderType,
    sources: List<AccountProviderSourceType>
  ): org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType {
    return AccountProviderRegistry.createFrom(this.context, sources, defaultProvider)
  }
}
