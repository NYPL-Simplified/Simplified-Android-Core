package org.nypl.simplified.tests.books.accounts

import android.content.Context
import org.mockito.Mockito
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountProviderSourceFileBasedTest : AccountProviderSourceFileBasedContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(AccountProviderSourceFileBasedTest::class.java)

  override val context: Context
    get() = Mockito.mock(Context::class.java)
}
