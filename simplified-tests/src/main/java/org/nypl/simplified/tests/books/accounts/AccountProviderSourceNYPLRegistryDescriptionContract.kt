package org.nypl.simplified.tests.books.accounts

import com.io7m.jfunctional.Option
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.BASIC_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.COPPA_TYPE
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.accounts.source.nyplregistry.AccountProviderSourceNYPLRegistryDescription
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParserType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectLink
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectNYPLFeatures
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectNYPLInput
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.tests.MockAccountProviderResolutionStrings
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

abstract class AccountProviderSourceNYPLRegistryDescriptionContract {

  private lateinit var mockHTTP: MockingHTTP
  private lateinit var authDocumentParser: AuthenticationDocumentParserType
  private lateinit var authDocumentParsers: AuthenticationDocumentParsersType
  private lateinit var stringResources: MockAccountProviderResolutionStrings

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceNYPLRegistryDescriptionContract::class.java)

  @Before
  fun testSetup() {
    this.stringResources = MockAccountProviderResolutionStrings()
    this.authDocumentParsers = Mockito.mock(AuthenticationDocumentParsersType::class.java)
    this.authDocumentParser = Mockito.mock(AuthenticationDocumentParserType::class.java)
    this.mockHTTP = MockingHTTP()
  }

  /**
   * Resolution fails if there is no authentication document URI.
   */

  @Test
  fun testMissingURI() {
    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentMissingURI", result.steps.last().resolution)
  }

  /**
   * Resolution fails the authentication document can't be retrieved.
   */

  @Test
  fun testAuthDocumentFetchFails() {
    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultError(
        400,
        "BAD REQUEST",
        0L,
        mapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()
      ))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentRetrievalFailed", result.steps.last().resolution)
  }

  /**
   * Resolution fails the authentication document can't be retrieved.
   */

  @Test
  fun testAuthDocumentFetchFailsException() {
    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultException(
        URI("http://www.example.com/auth"),
        IOException("Connection failed")
      ))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("Connection failed", result.steps.last().resolution)
  }

  /**
   * Resolution fails the authentication document can't be parsed.
   */

  @Test
  fun testAuthDocumentUnparseable() {
    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Failure(listOf(), listOf()))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentParseFailed", result.steps.last().resolution)
  }

  /**
   * Resolution succeeds if nothing fails.
   */

  @Test
  fun testAuthDocumentOK() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        authentication = listOf(AuthenticationObject(
          type = URI(BASIC_TYPE),
          description = "Basic Auth",
          labels = mapOf(
            Pair("LOGIN", "LOGIN!"),
            Pair("PASSWORD", "PASSWORD!")),
          inputs = mapOf(
            Pair("LOGIN", AuthenticationObjectNYPLInput(
              fieldName = "LOGIN",
              keyboardType = "DEFAULT",
              maximumLength = 20,
              barcodeFormat = "CODABAR"
            )),
            Pair("PASSWORD", AuthenticationObjectNYPLInput(
              fieldName = "PASSWORD",
              keyboardType = "DEFAULT",
              maximumLength = 20,
              barcodeFormat = "CODABAR"
            )))
        )),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/feed.xml"),
            rel = "start"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertFalse(result.failed)

    val provider = AccountProviderImmutable(
      addAutomatically = false,
      annotationsURI = null,
      authentication = AccountProviderAuthenticationDescription.Basic(
        "CODABAR",
        "DEFAULT",
        20,
        "DEFAULT",
        "Basic Auth",
        labels = mapOf(
          Pair("LOGIN", "LOGIN!"),
          Pair("PASSWORD", "PASSWORD!"))),
      authenticationDocumentURI = URI("http://www.example.com/auth"),
      cardCreatorURI = URI("http://www.example.com/card.xml"),
      catalogURI = URI("http://www.example.com/feed.xml"),
      displayName = "Title",
      eula = URI("http://www.example.com/eula.xml"),
      id = URI.create("urn:fake:0"),
      idNumeric = -1,
      isProduction = true,
      license = URI("http://www.example.com/license.xml"),
      loansURI = URI("http://www.example.com/shelf.xml"),
      logo = URI("http://www.example.com/logo.png"),
      mainColor = "blue",
      patronSettingsURI = URI("http://www.example.com/settings.xml"),
      privacyPolicy = URI("http://www.example.com/privacy.xml"),
      subtitle = "Some library you've never heard of",
      supportEmail = "mailto:someone@example.com",
      supportsReservations = true,
      supportsSimplyESynchronization = false,
      updated = DateTime.parse("1970-01-01T00:00:00.000Z"))

    Assert.assertEquals(provider, result.result)
  }

  /**
   * Resolution succeeds if nothing fails.
   */

  @Test
  fun testAuthDocumentOK_COPPA() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        authentication = listOf(AuthenticationObject(
          type = URI(COPPA_TYPE),
          description = "COPPA Age Gate",
          labels = mapOf(),
          inputs = mapOf(),
          links = listOf(
            AuthenticationObjectLink(
              href = URI("http://www.example.com/feed-13.xml"),
              rel = "http://librarysimplified.org/terms/rel/authentication/restriction-met"
            ),
            AuthenticationObjectLink(
              href = URI("http://www.example.com/feed-under-13.xml"),
              rel = "http://librarysimplified.org/terms/rel/authentication/restriction-not-met"
            ))
        )),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/feed.xml"),
            rel = "start"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertFalse(result.failed)

    val provider = AccountProviderImmutable(
      addAutomatically = false,
      annotationsURI = null,
      authentication = AccountProviderAuthenticationDescription.COPPAAgeGate(
        greaterEqual13 = URI("http://www.example.com/feed-13.xml"),
        under13 = URI("http://www.example.com/feed-under-13.xml")
      ),
      authenticationDocumentURI = URI("http://www.example.com/auth"),
      cardCreatorURI = URI("http://www.example.com/card.xml"),
      catalogURI = URI("http://www.example.com/feed.xml"),
      displayName = "Title",
      eula = URI("http://www.example.com/eula.xml"),
      id = URI.create("urn:fake:0"),
      idNumeric = -1,
      isProduction = true,
      license = URI("http://www.example.com/license.xml"),
      loansURI = URI("http://www.example.com/shelf.xml"),
      logo = URI("http://www.example.com/logo.png"),
      mainColor = "blue",
      patronSettingsURI = URI("http://www.example.com/settings.xml"),
      privacyPolicy = URI("http://www.example.com/privacy.xml"),
      subtitle = "Some library you've never heard of",
      supportEmail = "mailto:someone@example.com",
      supportsReservations = true,
      supportsSimplyESynchronization = false,
      updated = DateTime.parse("1970-01-01T00:00:00.000Z"))

    Assert.assertEquals(provider, result.result)
  }

  /**
   * Resolution succeeds if nothing fails.
   */

  @Test
  fun testAuthDocumentOK_NoAuth() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        authentication = listOf(),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/feed.xml"),
            rel = "start"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertFalse(result.failed)

    val provider = AccountProviderImmutable(
      addAutomatically = false,
      annotationsURI = null,
      authentication = null,
      authenticationDocumentURI = URI("http://www.example.com/auth"),
      cardCreatorURI = URI("http://www.example.com/card.xml"),
      catalogURI = URI("http://www.example.com/feed.xml"),
      displayName = "Title",
      eula = URI("http://www.example.com/eula.xml"),
      id = URI.create("urn:fake:0"),
      idNumeric = -1,
      isProduction = true,
      license = URI("http://www.example.com/license.xml"),
      loansURI = URI("http://www.example.com/shelf.xml"),
      logo = URI("http://www.example.com/logo.png"),
      mainColor = "blue",
      patronSettingsURI = URI("http://www.example.com/settings.xml"),
      privacyPolicy = URI("http://www.example.com/privacy.xml"),
      subtitle = "Some library you've never heard of",
      supportEmail = "mailto:someone@example.com",
      supportsReservations = true,
      supportsSimplyESynchronization = false,
      updated = DateTime.parse("1970-01-01T00:00:00.000Z"))

    Assert.assertEquals(provider, result.result)
  }

  /**
   * Resolution fails if the COPPA age gate lacks the correct links.
   */

  @Test
  fun testAuthDocumentFails_COPPAMalformed() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        authentication = listOf(AuthenticationObject(
          type = URI(COPPA_TYPE),
          description = "COPPA Age Gate",
          labels = mapOf(),
          inputs = mapOf(),
          links = listOf()
        )),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/feed.xml"),
            rel = "start"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentCOPPAAgeGateMalformed", result.steps.last().resolution)
  }

  /**
   * Resolution fails if the authentication document contains only authentication types that
   * we can't understand.
   */

  @Test
  fun testAuthDocumentFails_OnlyUnrecognizedAuth() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        authentication = listOf(AuthenticationObject(
          type = URI("urn:unknown"),
          description = "COPPA Age Gate",
          labels = mapOf(),
          inputs = mapOf(),
          links = listOf()
        )),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/feed.xml"),
            rel = "start"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentNoUsableAuthenticationTypes", result.steps.last().resolution)
  }

  /**
   * Resolution fails if the authentication document doesn't provide a catalog URI.
   */

  @Test
  fun testAuthDocumentFails_NoCatalogURI() {
    DateTimeUtils.setCurrentMillisFixed(0L)

    val metadata =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:fake:0"),
        title = "Title",
        updated = DateTime.parse("2019-07-09T08:33:40+00:00"),
        links = listOf(
          AccountProviderDescriptionMetadata.Link(
            URI.create("http://www.example.com/auth"),
            "application/vnd.opds.authentication.v1.0+json",
            false,
            null)
        ),
        images = listOf(),
        isProduction = true,
        isAutomatic = false)

    val description = AccountProviderSourceNYPLRegistryDescription(
      stringResources = this.stringResources,
      authDocumentParsers = this.authDocumentParsers,
      http = this.mockHTTP,
      metadata = metadata
    )

    this.mockHTTP.addResponse(
      "http://www.example.com/auth",
      HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(2, { 0 })) as InputStream,
        2,
        mapOf(),
        0L
      ))

    Mockito.`when`(
      this.authDocumentParsers.createParser(anyNotNull(), anyNotNull(), Mockito.anyBoolean()))
      .thenReturn(this.authDocumentParser)

    val authDocument =
      AuthenticationDocument(
        id = URI("http://www.example.com/auth"),
        title = "Auth",
        mainColor = "blue",
        description = "Some library you've never heard of",
        authentication = listOf(),
        features = AuthenticationObjectNYPLFeatures(
          enabled = setOf("https://librarysimplified.org/rel/policy/reservations"),
          disabled = setOf()
        ),
        links = listOf(
          AuthenticationObjectLink(
            href = URI("http://www.example.com/card.xml"),
            rel = "register"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/license.xml"),
            rel = "license"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/eula.xml"),
            rel = "terms-of-service"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/settings.xml"),
            rel = "http://librarysimplified.org/terms/rel/user-profile"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/privacy.xml"),
            rel = "privacy-policy"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/shelf.xml"),
            rel = "http://opds-spec.org/shelf"
          ),
          AuthenticationObjectLink(
            href = URI("mailto:someone@example.com"),
            rel = "help"
          ),
          AuthenticationObjectLink(
            href = URI("http://www.example.com/logo.png"),
            rel = "logo"
          )
        ))

    Mockito.`when`(this.authDocumentParser.parse())
      .thenReturn(ParseResult.Success(listOf(), authDocument))

    val result =
      description.resolve { _, message -> this.logger.debug("{}", message) }

    this.logger.debug("result: {}", result)
    Assert.assertTrue(result.failed)
    Assert.assertEquals("resolvingAuthDocumentNoStartURI", result.steps.last().resolution)
  }

  private fun <T> anyNotNull(): T {
    return Mockito.argThat<T> { x -> x != null }
  }
}
