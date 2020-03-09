package org.nypl.simplified.tests.android.books.accounts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.runner.RunWith
import org.nypl.simplified.tests.books.accounts.AccountAuthenticationCredentialsStoreContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@SmallTest
class AccountAuthenticationCredentialsStoreTest : AccountAuthenticationCredentialsStoreContract() {

  override val logger: Logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsStoreTest::class.java)
}
