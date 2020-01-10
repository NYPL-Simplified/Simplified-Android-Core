package org.nypl.simplified.tests.local.books.accounts

import org.nypl.simplified.tests.books.accounts.AccountProviderNYPLRegistryContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountProviderNYPLRegistryTest : AccountProviderNYPLRegistryContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AccountProviderNYPLRegistryTest::class.java)
}
