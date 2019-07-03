package org.nypl.simplified.tests.books.accounts

import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.accounts.source.api.AccountProviderSourceType.SourceResult.SourceFailed
import org.nypl.simplified.accounts.source.api.AccountProviderSourceType.SourceResult.SourceSucceeded
import org.nypl.simplified.accounts.source.filebased.AccountProviderSourceFileBased
import org.slf4j.Logger
import java.io.InputStream

abstract class AccountProviderSourceFileBasedContract {

  protected abstract val logger: Logger

  protected abstract val context: Context

  @Throws(Exception::class)
  private fun readAllFromResource(name: String): InputStream {
    return AccountProviderSourceFileBasedContract::class.java
      .getResource("/org/nypl/simplified/tests/books/accounts/$name")
      .openStream()
  }

  @Test
  fun testProvidersAll()
  {
    val provider = AccountProviderSourceFileBased(getFile = {
      this.readAllFromResource("providers-all.json")
    })

    val result = provider.load(this.context)
    this.logger.debug("status: {}", result)
    val success = result as SourceSucceeded
    Assert.assertEquals(172, success.results.size)
  }

}
