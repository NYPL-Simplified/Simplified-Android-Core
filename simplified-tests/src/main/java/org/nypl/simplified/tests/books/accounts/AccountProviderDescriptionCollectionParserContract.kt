package org.nypl.simplified.tests.books.accounts

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionParsers
import org.nypl.simplified.parser.api.ParseResult
import org.slf4j.Logger
import java.io.InputStream
import java.net.URI

abstract class AccountProviderDescriptionCollectionParserContract {

  protected abstract val logger : Logger

  @Throws(Exception::class)
  private fun resource(name: String): InputStream {
    return AccountProviderDescriptionCollectionParserContract::class.java
      .getResource("/org/nypl/simplified/tests/books/accounts/descriptions/$name")
      .openStream()
  }

  @Test
  fun testLibraryRegistry()
  {
    val parsers = AccountProviderDescriptionCollectionParsers()

    resource("libraryregistry.json").use { stream ->
      val parser = parsers.createParser(URI("urn:fake"), stream)
      val result = parser.parse()
      this.dumpResult(result)
      val success = result as ParseResult.Success
      val collection = success.result
      Assert.assertEquals(182, collection.providers.size)
    }
  }

  private fun dumpResult(result: ParseResult<AccountProviderDescriptionCollection>) {
    when (result) {
      is ParseResult.Success -> {
        result.warnings.forEach { warning ->
          this.logger.warn("warning: {}", warning)
        }
      }
      is ParseResult.Failure -> {
        result.errors.forEach { error ->
          this.logger.error("warning: {}", error)
        }
        result.warnings.forEach { warning ->
          this.logger.warn("warning: {}", warning)
        }
      }
    }
  }
}