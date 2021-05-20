package org.nypl.simplified.tests.books.profiles

import android.content.Context
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.drm.core.AdobeAdeptActivationReceiverType
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptProcedureType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.controller.ProfileAccountLoginTask
import org.nypl.simplified.patron.PatronUserProfileParsers
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.books.controller.TaskDumps
import org.nypl.simplified.tests.mocking.MockAccountLoginStringResources
import org.slf4j.Logger
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

abstract class ProfileAccountLoginTaskContract {

  private lateinit var account: AccountType
  private lateinit var accountID: AccountID
  private lateinit var adeptConnector: AdobeAdeptConnectorType
  private lateinit var adeptExecutor: AdobeAdeptExecutorType
  private lateinit var http: LSHTTPClientType
  private lateinit var loginStrings: AccountLoginStringResourcesType
  private lateinit var patronParserFactory: PatronUserProfileParsers
  private lateinit var profile: ProfileReadableType
  private lateinit var profileID: ProfileID
  private lateinit var profileWithDRM: String
  private lateinit var profileWithoutDRM: String
  private lateinit var server: MockWebServer

  private var loginState: AccountLoginState? = null

  abstract val logger: Logger

  @BeforeEach
  fun testSetup() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration(
            applicationName = "simplified-test",
            applicationVersion = "0.0.1",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.loginStrings =
      MockAccountLoginStringResources()
    this.adeptConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)
    this.adeptExecutor =
      Mockito.mock(AdobeAdeptExecutorType::class.java)
    this.patronParserFactory =
      PatronUserProfileParsers()

    this.accountID =
      AccountID(UUID.randomUUID())
    this.profileID =
      ProfileID(UUID.randomUUID())

    this.server = MockWebServer()
    this.server.start()

    this.profileWithoutDRM = """
{
  "simplified:authorization_identifier": "6120696828384",
  "drm": [],
  "simplified:authorization_expires": "2019-08-02T00:00:00Z",
  "settings": {
    "simplified:synchronize_annotations": true
  },
  "links": [
    {
       "href" : "https://www.example.com",
       "rel" : "http://www.w3.org/ns/oa#annotationService",
       "type" : "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\""
    }
  ]
}
"""

    this.profileWithDRM = """
{
  "simplified:authorization_identifier": "6120696828384",
  "drm": [
    {
      "drm:vendor": "OmniConsumerProducts",
      "drm:scheme": "http://librarysimplified.org/terms/drm/scheme/ACS",
      "drm:clientToken": "NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K",
      "links": [
        {
          "rel": "http://librarysimplified.org/terms/drm/rel/devices",
          "href": "http://${this.server.hostName}:${this.server.port}/devices"
        }
      ]
    }
  ],
  "links": [
    {
       "href" : "https://www.example.com",
       "rel" : "http://www.w3.org/ns/oa#annotationService",
       "type" : "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\""
    }
  ],
  "simplified:authorization_expires": "2019-08-02T00:00:00Z",
  "settings": {
    "simplified:synchronize_annotations": true
  }
}"""
  }

  @AfterEach
  fun testTearDown() {
    this.server.close()
  }

  /**
   * Logging in to an account that doesn't require logins doesn't work.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginNotRequired() {
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 8,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Description",
          labels = mapOf(),
          logoURI = null
        ),
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.authentication)
      .thenReturn(null)
    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Failure

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("loginAuthNotRequired", state.taskResult.lastErrorCode)

    assertEquals(0, this.server.requestCount)
  }

  /**
   * If the server responds with a 401, logging in fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginServer401() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(401)
        .setBody("this new chaos is entirely terrible, mindless, obeying rules that i don't comprehend. and it is hungry.")
    )

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("invalidCredentials", state.taskResult.lastErrorCode)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * If the server responds with a non-401 error, logging in fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginServerNon401() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(404)
        .setBody("this new chaos is entirely terrible, mindless, obeying rules that i don't comprehend. and it is hungry.")
    )

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("httpError 404 " + this.server.url("patron"), state.taskResult.lastErrorCode)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * If a connection attempt to the server results in an exception, logging in fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginServerException() {
    this.server.close()

    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("connectionFailed", state.taskResult.lastErrorCode)
  }

  /**
   * If no patron URI is provided, logging in fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginNoPatronURI() {
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 8,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Description",
          labels = mapOf(),
          logoURI = null
        ),
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(null)

    Mockito.`when`(provider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = "CODABAR",
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 10,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Library Login",
          labels = mapOf(),
          logoURI = null
        )
      )

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state =
      this.account.loginState as AccountLoginFailed

    assertEquals(0, this.server.requestCount)
  }

  /**
   * If a patron user profile cannot be parsed, logging in fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginPatronProfileUnparseable() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("a cracked eggshell lying on the grass")
    )

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("parseErrorPatronSettings", state.taskResult.lastErrorCode)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * If a patron user profile can be parsed and it advertises no DRM, then logging in succeeds.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginNoDRM() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state =
      this.account.loginState as AccountLoggedIn

    assertEquals(
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        adobeCredentials = null,
        authenticationDescription = "Description",
        annotationsURI = URI("https://www.example.com")
      ),
      state.credentials
    )

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * If a patron user profile can be parsed and it advertises Adobe DRM, and Adobe DRM is supported,
   * then logging in succeeds.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginAdobeDRM() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = "CODABAR",
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 10,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Library Login",
        labels = mapOf(),
        logoURI = null
      )

    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())

    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithDRM.trimIndent())
    )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
    )

    /*
     * When the code calls activateDevice(), it succeeds if the connector returns a single
     * activation.
     */

    Mockito.`when`(
      this.adeptConnector.activateDevice(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptActivationReceiverType
      receiver.onActivationsCount(1)
      receiver.onActivation(
        0,
        AdobeVendorID("OmniConsumerProducts"),
        AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
        "user",
        AdobeUserID("someone"),
        null
      )
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = this.adeptExecutor,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state =
      this.account.loginState as AccountLoggedIn

    val originalCredentials =
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        adobeCredentials = null,
        authenticationDescription = "Library Login",
        annotationsURI = URI("https://www.example.com")
      )

    val newCredentials =
      originalCredentials.withAdobePreActivationCredentials(
        AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("devices").toUri(),
          postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
            deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
            userID = AdobeUserID("someone")
          )
        )
      )

    assertEquals(newCredentials, state.credentials)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    val req1 = this.server.takeRequest()
    assertEquals(this.server.url("devices"), req1.requestUrl)

    assertEquals(2, this.server.requestCount)
  }

  /**
   * If the account shows that DRM is required, but none is supported, then fail.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginAdobeDRMNotSupported() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )

    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithDRM.trimIndent())
    )

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state =
      this.account.loginState as AccountLoggedIn

    val originalCredentials =
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        adobeCredentials = null,
        authenticationDescription = "Description",
        annotationsURI = URI("https://www.example.com")
      )

    val newCredentials =
      originalCredentials.withAdobePreActivationCredentials(
        AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("devices").toUri(),
          postActivationCredentials = null
        )
      )

    assertEquals(newCredentials, state.credentials)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * If no activations are delivered by the Adobe DRM connector, then activation fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginAdobeDRMNoActivations() {
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 8,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Description",
          labels = mapOf(),
          logoURI = null
        ),
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())

    Mockito.`when`(provider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = "CODABAR",
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 10,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Library Login",
          labels = mapOf(),
          logoURI = null
        )
      )

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithDRM.trimIndent())
    )

    /*
     * When the code calls activateDevice(), it succeeds if the connector returns a single
     * activation.
     */

    Mockito.`when`(
      this.adeptConnector.activateDevice(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver =
        invocation.arguments[0] as AdobeAdeptActivationReceiverType
      Unit
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = this.adeptExecutor,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed

    assertEquals(0, this.server.requestCount)
  }

  /**
   * If the Adobe DRM connector delivers multiple activations, the one that is not associated with
   * any other accounts is used.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginAdobeDRMMultipleActivations() {
    val previouslyLoggedInAccountId = AccountID(UUID.randomUUID())
    val previouslyLoggedInAccount = Mockito.mock(AccountType::class.java, Mockito.RETURNS_DEEP_STUBS)

    Mockito.`when`(
      previouslyLoggedInAccount.loginState.credentials?.adobeCredentials?.postActivationCredentials?.userID
    ).thenReturn(AdobeUserID("someone"))

    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = "CODABAR",
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 10,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Library Login",
        labels = mapOf(),
        logoURI = null
      )

    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())

    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(
        sortedMapOf(
          Pair(previouslyLoggedInAccountId, previouslyLoggedInAccount),
          Pair(this.accountID, this.account)
        )
      )
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithDRM.trimIndent())
    )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
    )

    Mockito.`when`(
      this.adeptConnector.activateDevice(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptActivationReceiverType
      receiver.onActivationsCount(2)
      receiver.onActivation(
        0,
        AdobeVendorID("OmniConsumerProducts"),
        AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
        "user",
        AdobeUserID("someone"),
        null
      )
      receiver.onActivation(
        1,
        AdobeVendorID("OmniConsumerProducts"),
        AdobeDeviceID("deb213da-3dd1-4978-9642-03d1f288154c"),
        "another.user",
        AdobeUserID("someone.else"),
        null
      )
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = this.adeptExecutor,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state =
      this.account.loginState as AccountLoggedIn

    val originalCredentials =
      AccountAuthenticationCredentials.Basic(
        userName = request.username,
        password = request.password,
        adobeCredentials = null,
        authenticationDescription = "Library Login",
        annotationsURI = URI("https://www.example.com")
      )

    val newCredentials =
      originalCredentials.withAdobePreActivationCredentials(
        AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("devices").toUri(),
          postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
            deviceID = AdobeDeviceID("deb213da-3dd1-4978-9642-03d1f288154c"),
            userID = AdobeUserID("someone.else")
          )
        )
      )

    assertEquals(newCredentials, state.credentials)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    val req1 = this.server.takeRequest()
    assertEquals(this.server.url("devices"), req1.requestUrl)

    assertEquals(2, this.server.requestCount)
  }

  /**
   * If the Adobe DRM connector delivers an error, then activation fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginAdobeDRMActivationError() {
    val authDescription =
      AccountProviderAuthenticationDescription.Basic(
        barcodeFormat = null,
        keyboard = KeyboardInput.DEFAULT,
        passwordMaximumLength = 8,
        passwordKeyboard = KeyboardInput.DEFAULT,
        description = "Description",
        labels = mapOf(),
        logoURI = null
      )
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = authDescription,
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())

    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithDRM.trimIndent())
    )

    /*
     * When the code calls activateDevice(), it fails if the connector returns an error.
     */

    Mockito.`when`(
      this.adeptConnector.activateDevice(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptActivationReceiverType
      receiver.onActivationError("E_FAIL_OFTEN_AND_LOUDLY")
    }

    Mockito.`when`(this.adeptExecutor.execute(this.anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = this.adeptExecutor,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("Adobe ACS: E_FAIL_OFTEN_AND_LOUDLY", state.taskResult.lastErrorCode)

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * Trying to log in to an account using an unsupported mechanism, fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginWrongType0() {
    val request =
      ProfileAccountLoginRequest.Basic(
        accountId = this.accountID,
        description = AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 8,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Description",
          labels = mapOf(),
          logoURI = null
        ),
        username = AccountUsername("user"),
        password = AccountPassword("password")
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.OAuthWithIntermediary(
          description = "Description",
          logoURI = null,
          authenticate = URI.create("urn:example")
        )
      )

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Failure

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("loginAuthNotRequired", state.taskResult.lastErrorCode)

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Trying to log in to an account using an unsupported mechanism, fails.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginWrongType1() {
    val request =
      ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate(
        accountId = this.accountID,
        description = AccountProviderAuthenticationDescription.OAuthWithIntermediary(
          description = "Description",
          logoURI = null,
          authenticate = URI.create("urn:example")
        )
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.authentication)
      .thenReturn(
        AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = KeyboardInput.DEFAULT,
          passwordMaximumLength = 8,
          passwordKeyboard = KeyboardInput.DEFAULT,
          description = "Description",
          labels = mapOf(),
          logoURI = null
        )
      )

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    val task =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request
      )

    val result = task.call()
    TaskDumps.dump(this.logger, result)
    result as TaskResult.Failure

    val state = this.account.loginState as AccountLoginFailed
    assertEquals("loginAuthNotRequired", state.taskResult.lastErrorCode)

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Logging in with OAuth succeeds.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginOAuthCompleteNoDRM() {
    val authDescription =
      AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate(
        accountId = this.accountID,
        description = authDescription
      )
    val request1 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryComplete(
        accountId = this.accountID,
        token = "A TOKEN!"
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    val result0 = task0.call()
    TaskDumps.dump(this.logger, result0)

    this.account.loginState as AccountLoggingInWaitingForExternalAuthentication

    val task1 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request1
      )

    val result1 = task1.call()
    TaskDumps.dump(this.logger, result1)

    val state =
      this.account.loginState as AccountLoggedIn

    assertEquals(
      AccountAuthenticationCredentials.OAuthWithIntermediary(
        adobeCredentials = null,
        authenticationDescription = "Description",
        accessToken = "A TOKEN!",
        annotationsURI = URI("https://www.example.com")
      ),
      state.credentials
    )

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * Receiving an OAuth token in an account that wasn't waiting for one ignores the request.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginOAuthNotWaiting() {
    val authDescription =
      AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryComplete(
        accountId = this.accountID,
        token = "A TOKEN!"
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    this.loginState = AccountNotLoggedIn

    val result0 = task0.call() as TaskResult.Success
    TaskDumps.dump(this.logger, result0)

    this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Cancelling an OAuth request in an account that wasn't waiting for one ignores the request.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginOAuthNotWaitingCancel() {
    val authDescription =
      AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryCancel(
        accountId = this.accountID,
        description = authDescription
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    this.loginState = AccountNotLoggedIn

    val result0 = task0.call() as TaskResult.Success
    TaskDumps.dump(this.logger, result0)

    this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Cancelling OAuth works.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testLoginOAuthCancel() {
    val authDescription =
      AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate(
        accountId = this.accountID,
        description = authDescription
      )
    val request1 =
      ProfileAccountLoginRequest.OAuthWithIntermediaryCancel(
        accountId = this.accountID,
        description = authDescription
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(this.anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    val result0 = task0.call()
    TaskDumps.dump(this.logger, result0)

    this.account.loginState as AccountLoggingInWaitingForExternalAuthentication

    val task1 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request1
      )

    val result1 = task1.call()
    TaskDumps.dump(this.logger, result1)

    val state =
      this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Logging in with SAML20 succeeds.
   */

  @Test
  fun testLoginSAML20CompleteNoDRM() {
    val authDescription =
      AccountProviderAuthenticationDescription.SAML2_0(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.SAML20Initiate(
        accountId = this.accountID,
        description = authDescription
      )
    val request1 =
      ProfileAccountLoginRequest.SAML20Complete(
        accountId = this.accountID,
        accessToken = "A TOKEN!",
        patronInfo = "{}",
        cookies = listOf(
          AccountCookie("https://example", "cookie0=23"),
          AccountCookie("https://fake", "cookie1=24; Path=/; Secure"),
          AccountCookie("http://something", "cookie2=25; Path=/abc; Expires=Wed, 23 Dec 2020 07:28:00 GMT")
        )
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    val result0 = task0.call()
    TaskDumps.dump(logger, result0)

    this.account.loginState as AccountLoggingInWaitingForExternalAuthentication

    val task1 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request1
      )

    val result1 = task1.call()
    TaskDumps.dump(logger, result1)

    val state =
      this.account.loginState as AccountLoggedIn

    Assertions.assertEquals(
      AccountAuthenticationCredentials.SAML2_0(
        adobeCredentials = null,
        authenticationDescription = "Description",
        accessToken = "A TOKEN!",
        patronInfo = "{}",
        annotationsURI = URI("https://www.example.com"),
        cookies = listOf(
          AccountCookie("https://example", "cookie0=23"),
          AccountCookie("https://fake", "cookie1=24; Path=/; Secure"),
          AccountCookie("http://something", "cookie2=25; Path=/abc; Expires=Wed, 23 Dec 2020 07:28:00 GMT")
        )
      ),
      state.credentials
    )

    val req0 = this.server.takeRequest()
    assertEquals(this.server.url("patron"), req0.requestUrl)
    assertEquals(1, this.server.requestCount)
  }

  /**
   * Receiving an SAML20 token in an account that wasn't waiting for one ignores the request.
   */

  @Test
  fun testLoginSAML20NotWaiting() {
    val authDescription =
      AccountProviderAuthenticationDescription.SAML2_0(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.SAML20Complete(
        accountId = this.accountID,
        accessToken = "A TOKEN!",
        patronInfo = "{}",
        cookies = listOf(
          AccountCookie("https://example", "cookie0=23"),
          AccountCookie("https://fake", "cookie1=24; Path=/; Secure"),
          AccountCookie("http://something", "cookie2=25; Path=/abc; Expires=Wed, 23 Dec 2020 07:28:00 GMT")
        )
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    this.loginState = AccountNotLoggedIn

    val result0 = task0.call() as TaskResult.Success
    TaskDumps.dump(logger, result0)

    this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Cancelling an SAML20 request in an account that wasn't waiting for one ignores the request.
   */

  @Test
  fun testLoginSAML20NotWaitingCancel() {
    val authDescription =
      AccountProviderAuthenticationDescription.SAML2_0(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.SAML20Cancel(
        accountId = this.accountID,
        description = authDescription
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    this.loginState = AccountNotLoggedIn

    val result0 = task0.call() as TaskResult.Success
    TaskDumps.dump(logger, result0)

    this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  /**
   * Cancelling SAML20 works.
   */

  @Test
  fun testLoginSAML20Cancel() {
    val authDescription =
      AccountProviderAuthenticationDescription.SAML2_0(
        description = "Description",
        logoURI = null,
        authenticate = URI.create("urn:example")
      )
    val request0 =
      ProfileAccountLoginRequest.SAML20Initiate(
        accountId = this.accountID,
        description = authDescription
      )
    val request1 =
      ProfileAccountLoginRequest.SAML20Cancel(
        accountId = this.accountID,
        description = authDescription
      )

    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())
    Mockito.`when`(provider.authentication)
      .thenReturn(authDescription)

    Mockito.`when`(this.profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(this.profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.accountID, this.account)))
    Mockito.`when`(this.account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(this.account.provider)
      .thenReturn(provider)
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(this.profileWithoutDRM.trimIndent())
    )

    val task0 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request0
      )

    val result0 = task0.call()
    TaskDumps.dump(logger, result0)

    this.account.loginState as AccountLoggingInWaitingForExternalAuthentication

    val task1 =
      ProfileAccountLoginTask(
        adeptExecutor = null,
        http = this.http,
        profile = this.profile,
        account = this.account,
        loginStrings = this.loginStrings,
        patronParsers = this.patronParserFactory,
        request = request1
      )

    val result1 = task1.call()
    TaskDumps.dump(logger, result1)

    val state =
      this.account.loginState as AccountNotLoggedIn

    assertEquals(0, this.server.requestCount)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }
}
