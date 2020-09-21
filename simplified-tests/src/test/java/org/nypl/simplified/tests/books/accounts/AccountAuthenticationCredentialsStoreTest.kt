package org.nypl.simplified.tests.books.accounts

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountAuthenticationCredentialsStoreTest : AccountAuthenticationCredentialsStoreContract() {

  override val logger: Logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsStoreTest::class.java)
}
