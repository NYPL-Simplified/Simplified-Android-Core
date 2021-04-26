package org.nypl.simplified.tests.opds2

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.opds2.parser.api.OPDS2ParsersType
import org.nypl.simplified.parser.api.ParseResult
import org.slf4j.Logger
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

abstract class OPDS2ParserContract {

  private lateinit var parsers: OPDS2ParsersType

  abstract val logger: Logger

  abstract fun createParsers(): OPDS2ParsersType

  @BeforeEach
  fun setup() {
    this.parsers = this.createParsers()
  }

  @Throws(Exception::class)
  private fun resource(name: String): InputStream {
    return resourceByPath("/org/nypl/simplified/tests/opds2/$name")
  }

  @Throws(Exception::class)
  private fun resourceByPath(path: String): InputStream {
    val url = OPDS2ParserContract::class.java.getResource(path)
      ?: throw FileNotFoundException(path)
    return url.openStream()
  }

  /**
   * Parsing the registry yields the right number of entries.
   */

  @Test
  fun testRegistry() {
    val path = "/org/nypl/simplified/tests/books/accounts/descriptions/libraryregistry-qa.json"
    this.resourceByPath(path).use { stream ->
      this.parsers.createParser(URI.create("urn:test"), stream, warningsAsErrors = true).use { parser ->
        val success = parser.parse() as ParseResult.Success
        val feed = success.result
        Assertions.assertEquals(182, feed.catalogs.size)
      }
    }
  }
}
