package org.nypl.simplified.tests.books.accounts

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionParsers
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionSerializers
import org.nypl.simplified.parser.api.ParseResult
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI

abstract class AccountProviderDescriptionCollectionParserContract {

  protected abstract val logger: Logger

  @Throws(Exception::class)
  private fun resource(name: String): InputStream {
    return AccountProviderDescriptionCollectionParserContract::class.java
      .getResource("/org/nypl/simplified/tests/books/accounts/descriptions/$name")!!
      .openStream()
  }

  /**
   * Check that parsing some input gives the right data.
   */

  @Test
  fun testLibraryRegistry() {
    val parsers = AccountProviderDescriptionCollectionParsers()

    resource("libraryregistry-qa.json").use { stream ->
      val parser = parsers.createParser(URI("urn:fake"), stream)
      val result = parser.parse()
      this.dumpResult(result)
      val success = result as ParseResult.Success
      val collection = success.result
      Assert.assertEquals(182, collection.providers.size)
      Assert.assertTrue(collection.providers.any { p -> p.links.isNotEmpty() })
      Assert.assertTrue(collection.providers.any { p -> p.images.isNotEmpty() })
      Assert.assertEquals(4, collection.links.size)
      Assert.assertEquals("NYPL", collection.metadata.adobeVendorID!!.value)
      Assert.assertEquals("Libraries", collection.metadata.title)
    }
  }

  /**
   * Check that parsing and then serializing a collection results in the same collection.
   */

  @Test
  fun testLibraryRegistryRoundTrip() {
    val parsers = AccountProviderDescriptionCollectionParsers()
    val serializers = AccountProviderDescriptionCollectionSerializers()

    resource("libraryregistry-qa.json").use { stream ->
      val parser = parsers.createParser(URI("urn:fake"), stream)
      val result = parser.parse()
      this.dumpResult(result)
      val success = result as ParseResult.Success
      val collection = success.result

      val output = ByteArrayOutputStream()
      val serializer =
        serializers.createSerializer(URI("urn:fake"), output, collection)
      serializer.serialize()

      val parserSerialized =
        parsers.createParser(URI("urn:fake"), ByteArrayInputStream(output.toByteArray()))
      val resultSerialized =
        parserSerialized.parse()

      val successSerialized = resultSerialized as ParseResult.Success
      val collectionSerialized = success.result
      Assert.assertEquals(collection, collectionSerialized)
    }
  }

  private fun dumpResult(result: ParseResult<AccountProviderDescriptionCollection>) {
    when (result) {
      is ParseResult.Success -> {
        result.warnings.forEach { warning ->
          this.logger.warn("warning: {}: ", warning, warning.exception)
        }
      }
      is ParseResult.Failure -> {
        result.errors.forEach { error ->
          this.logger.error("warning: {}: ", error, error.exception)
        }
        result.warnings.forEach { warning ->
          this.logger.warn("warning: {}: ", warning, warning.exception)
        }
      }
    }
  }
}
