package org.nypl.simplified.tests.books.accounts

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountProviderNYPLRegistryTest : AccountProviderNYPLRegistryContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AccountProviderNYPLRegistryTest::class.java)
}
