package org.nypl.simplified.tests.local.books.accounts

import org.nypl.simplified.tests.books.accounts.AccountAuthenticationCredentialsStoreContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountAuthenticationCredentialsStoreTest : AccountAuthenticationCredentialsStoreContract() {

  override val logger: Logger =
              LoggerFactory.getLogger(AccountAuthenticationCredentialsStoreTest::class.java)
}
