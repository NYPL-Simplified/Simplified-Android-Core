package org.nypl.simplified.tests.books.profiles

import com.io7m.jfunctional.Option
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginConnectionFailure
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginCredentialsIncorrect
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginNotRequired
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginServerError
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData.AccountLoginServerParseError
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.controller.ProfileAccountLoginTask
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronSettings
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParserType
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.tests.MockAccountLoginStringResources
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.util.UUID

abstract class ProfileAccountLoginTaskContract {

  private lateinit var profileID: ProfileID
  private lateinit var accountID: AccountID
  private var loginState: AccountLoginState? = null
  private lateinit var patronParsers: PatronUserProfileParsersType
  private lateinit var loginStrings: AccountLoginStringResourcesType
  private lateinit var account: AccountType
  private lateinit var profile: ProfileReadableType
  private lateinit var http: MockingHTTP

  abstract val logger: Logger

  @Before
  fun testSetup() {
    this.http = MockingHTTP()
    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.loginStrings =
      MockAccountLoginStringResources()
    this.patronParsers =
      Mockito.mock(PatronUserProfileParsersType::class.java)

    this.accountID =
      AccountID(UUID.randomUUID())
    this.profileID =
      ProfileID(UUID.randomUUID())
  }

  @After
  fun testTearDown() {

  }

  /**
   * Logging in to an account that doesn't require logins doesn't work.
   */

  @Test
  fun testLoginNotRequired() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.authentication)
      .thenReturn(null)
    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed

    Assert.assertEquals(AccountLoginNotRequired, state.steps.last().errorValue)
  }

  /**
   * If the server responds with a 401, logging in fails.
   */

  @Test
  fun testLoginServer401() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultError(
        401,
        "UNAUTHORIZED",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()
      ))

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed

    Assert.assertEquals(AccountLoginCredentialsIncorrect, state.steps.last().errorValue)
  }

  /**
   * If the server responds with a non-401 error, logging in fails.
   */

  @Test
  fun testLoginServerNon401() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultError(
        404,
        "NOT FOUND",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()
      ))

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed

    Assert.assertEquals(AccountLoginServerError(
      URI.create("urn:patron"),
      404,
      "NOT FOUND",
      null
    ), state.steps.last().errorValue)
  }

  /**
   * If a connection attempt to the server results in an exception, logging in fails.
   */

  @Test
  fun testLoginServerException() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultException(
        URI("urn:patron"),
        Exception()
      ))

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed

    Assert.assertEquals(AccountLoginConnectionFailure, state.steps.last().errorValue)
  }

  /**
   * If no patron URI is provided, logging in fails.
   */

  @Test
  fun testLoginNoPatronURI() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(null)

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultException(
        URI("urn:patron"),
        Exception()
      ))

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed
  }

  /**
   * If a patron user profile cannot be parsed, logging in fails.
   */

  @Test
  fun testLoginPatronProfileUnparseable() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    val parser =
      Mockito.mock(PatronUserProfileParserType::class.java)

    val patronStream =
      Mockito.mock(InputStream::class.java)

    val parseWarnings =
      listOf(ParseWarning(URI.create("urn:patron"), "Warning", exception = Exception()))
    val parseErrors =
      listOf(ParseError(URI.create("urn:patron"), "Error", exception = Exception()))

    Mockito.`when`(parser.parse())
      .thenReturn(ParseResult.Failure(parseWarnings, parseErrors))
    Mockito.`when`(this.patronParsers.createParser(URI.create("urn:patron"), patronStream, false))
      .thenReturn(parser)

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultOK(
        "OK",
        200,
        patronStream,
        0L,
        mutableMapOf(),
        0L
      ) as HTTPResultType<InputStream>)

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoginFailed

    Assert.assertEquals(
      AccountLoginServerParseError(parseWarnings, parseErrors),
      state.steps.last().errorValue)
  }

  /**
   * If a patron user profile can be parsed and it advertises no DRM, then logging in succeeds.
   */

  @Test
  fun testLoginNoDRM() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    val parser =
      Mockito.mock(PatronUserProfileParserType::class.java)

    val patronStream =
      Mockito.mock(InputStream::class.java)

    val profile =
      PatronUserProfile(
        settings = PatronSettings(false),
        drm = listOf(),
        authorization = null)

    Mockito.`when`(parser.parse())
      .thenReturn(ParseResult.Success(listOf(), profile))
    Mockito.`when`(this.patronParsers.createParser(URI.create("urn:patron"), patronStream, false))
      .thenReturn(parser)

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultOK(
        "OK",
        200,
        patronStream,
        0L,
        mutableMapOf(),
        0L
      ) as HTTPResultType<InputStream>)

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoggedIn

    Assert.assertEquals(credentials, state.credentials)
  }

  /**
   * If a patron user profile can be parsed and it advertises Adobe DRM, then logging in succeeds.
   */

  @Test
  fun testLoginAdobeDRM() {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(URI.create("urn:patron"))

    Mockito.`when`(provider.authentication)
      .thenReturn(AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create("urn:auth"))
        .setPassCodeLength(10)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())

    Mockito.`when`(this.profile.id())
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id())
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider())
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState())
      .then { this.loginState }

    val parser =
      Mockito.mock(PatronUserProfileParserType::class.java)

    val patronStream =
      Mockito.mock(InputStream::class.java)

    val profile =
      PatronUserProfile(
        settings = PatronSettings(false),
        drm = listOf(PatronDRMAdobe(
          vendor = "OmniConsumerProducts",
          scheme = URI("http://librarysimplified.org/terms/drm/scheme/ACS"),
          clientToken = "NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K",
          deviceManagerURI = URI("https://example.com/devices")
        )),
        authorization = null)

    Mockito.`when`(parser.parse())
      .thenReturn(ParseResult.Success(listOf(), profile))
    Mockito.`when`(this.patronParsers.createParser(URI.create("urn:patron"), patronStream, false))
      .thenReturn(parser)

    this.http.addResponse(
      URI.create("urn:patron"),
      HTTPResultOK(
        "OK",
        200,
        patronStream,
        0L,
        mutableMapOf(),
        0L
      ) as HTTPResultType<InputStream>)

    val task =
      ProfileAccountLoginTask(
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParsers,
        initialCredentials = credentials)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountLoggedIn

    val newCredentials =
      credentials.toBuilder()
        .setAdobeCredentials(
          AccountAuthenticationAdobePreActivationCredentials(
            vendorID = AdobeVendorID("OmniConsumerProducts"),
            clientToken = AccountAuthenticationAdobeClientToken.create("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
            deviceManagerURI = URI("https://example.com/devices"),
            postActivationCredentials = null
          )).build()

    Assert.assertEquals(newCredentials, state.credentials)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }
}