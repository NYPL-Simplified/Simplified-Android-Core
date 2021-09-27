package org.nypl.simplified.tests.books.accounts

import android.content.Context
import android.content.res.Resources
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionParsers
import org.nypl.simplified.accounts.json.AccountProviderDescriptionCollectionSerializers
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistry
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult.SourceSucceeded
import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds2.irradia.OPDS2ParsersIrradia
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class AccountProviderNYPLRegistryTest {

  private lateinit var server: MockWebServer
  private lateinit var http: LSHTTPClientType
  private lateinit var resources: Resources
  private lateinit var cacheDir: File
  private lateinit var context: Context

  private val logger: Logger =
    LoggerFactory.getLogger(AccountProviderNYPLRegistryTest::class.java)

  private val opdsParsers = OPDS2ParsersIrradia

  @Throws(Exception::class)
  private fun readAllFromResource(name: String): InputStream {
    return AccountProviderNYPLRegistryTest::class.java
      .getResource("/org/nypl/simplified/tests/books/accounts/descriptions/$name")!!
      .openStream()
  }

  @BeforeEach
  fun testSetup() {
    this.resources = Mockito.mock(Resources::class.java)
    this.context = Mockito.mock(Context::class.java)

    this.cacheDir = File.createTempFile("account-provider-nypl-registry", "dir")
    this.cacheDir.delete()
    this.cacheDir.mkdirs()

    Mockito.`when`(this.context.cacheDir)
      .thenReturn(this.cacheDir)
    Mockito.`when`(this.context.resources)
      .thenReturn(this.resources)
    Mockito.`when`(this.resources.getString(Mockito.anyInt()))
      .thenReturn("A STRING RESOURCE")

    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration(
            applicationName = "simplified-tests",
            applicationVersion = "1.0",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

    this.server = MockWebServer()
    this.server.start()
  }

  @AfterEach
  fun testTearDown() {
    this.cacheDir.deleteRecursively()
    this.server.close()
  }

  /**
   * The correct providers are returned from the server.
   */

  @Test
  fun testProductionProvidersFromServerOK() {
    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry.json")))
    )

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    val result = provider.load(this.context, false)
    this.logger.debug("status: {}", result)
    val success = result as SourceSucceeded

    Assertions.assertEquals(43, success.results.size)
  }

  /**
   * The correct providers are returned from the server.
   */

  @Test
  fun testAllProvidersFromServerOK() {
    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry-qa.json")))
    )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry.json")))
    )

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    val result = provider.load(this.context, true)
    this.logger.debug("status: {}", result)
    val success = result as SourceSucceeded

    Assertions.assertEquals(182, success.results.size)
  }

  /**
   * The correct providers are returned from the disk cache if one exists.
   */

  @Test
  fun testProvidersFromDiskCacheOK() {
    val cacheFile = File(this.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json")
    cacheFile.outputStream().use { output ->
      this.readAllFromResource("libraryregistry.json").use { input -> input.copyTo(output) }
    }

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    val result = provider.load(this.context, true)
    this.logger.debug("status: {}", result)
    val success = result as SourceSucceeded

    Assertions.assertEquals(43, success.results.size)
  }

  /**
   * If the disk cache contains garbage, the correct providers are fetched from the server,
   * and the disk cache is replaced with good data.
   */

  @Test
  fun testProvidersFromServerOKBadDiskCache() {
    val cacheFile = File(this.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json")
    cacheFile.outputStream().use { output -> output.write("Nonsense!".toByteArray()) }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry-qa.json")))
    )

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    val result = provider.load(this.context, true)
    this.logger.debug("status: {}", result)
    val success = result as SourceSucceeded

    Assertions.assertEquals(182, success.results.size)
    Assertions.assertNotEquals("Nonsense!", cacheFile.readText())
  }

  /**
   * If the server returns garbage and the disk cache doesn't exist, the source fails.
   */

  @Test
  fun testProvidersBadEverywhere() {
    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("Nonsense! Tripe! Ungood data!")
    )

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    val result = provider.load(this.context, true)
    this.logger.debug("status: {}", result)
    val failed = result as AccountProviderSourceType.SourceResult.SourceFailed

    Assertions.assertEquals(0, failed.results.size)
  }

  /**
   * The providers are queried from the server if the cache is expired.
   */

  @Test
  fun testProvidersRefreshOK() {
    val cacheFile = File(this.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json")
    cacheFile.outputStream().use { output ->
      this.readAllFromResource("libraryregistry.json").use { input -> input.copyTo(output) }
    }

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    this.run {
      val result = provider.load(this.context, true)
      this.logger.debug("status: {}", result)
      val success = result as SourceSucceeded

      Assertions.assertEquals(43, success.results.size)
    }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry-qa.json")))
    )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().readFrom(this.readAllFromResource("libraryregistry.json")))
    )

    // Expire the cache
    DateTimeUtils.setCurrentMillisOffset(1000 * 43200)

    this.run {
      val result = provider.load(this.context, true)
      this.logger.debug("status: {}", result)
      val success = result as SourceSucceeded

      Assertions.assertEquals(182, success.results.size)
    }

    // Reset
    DateTimeUtils.setCurrentMillisSystem()
  }

  /**
   * Test that the disk cache can be cleared.
   */

  @Test
  fun testClearDiskCacheOK() {
    val cacheFile = File(this.cacheDir, "org.nypl.simplified.accounts.source.nyplregistry.json")
    cacheFile.outputStream().use { output ->
      this.readAllFromResource("libraryregistry.json").use { input -> input.copyTo(output) }
    }

    val provider =
      AccountProviderSourceNYPLRegistry(
        http = this.http,
        authDocumentParsers = AuthenticationDocumentParsers(),
        parsers = AccountProviderDescriptionCollectionParsers(this.opdsParsers),
        serializers = AccountProviderDescriptionCollectionSerializers(),
        uriProduction = this.server.url("production").toUri(),
        uriQA = this.server.url("qa").toUri()
      )

    this.run {
      val result1 = provider.load(this.context, true)
      this.logger.debug("status: {}", result1)
      val success1 = result1 as SourceSucceeded
      Assertions.assertEquals(43, success1.results.size)

      // Bust the cache
      provider.clear(this.context)
      Assertions.assertFalse(cacheFile.exists())
    }
  }
}
