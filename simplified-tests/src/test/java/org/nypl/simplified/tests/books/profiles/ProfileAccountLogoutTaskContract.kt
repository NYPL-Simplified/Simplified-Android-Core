package org.nypl.simplified.tests.books.profiles

import android.content.Context
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptProcedureType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.ProfileAccountLogoutTask
import org.nypl.simplified.patron.PatronUserProfileParsers
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.tests.MockAccountLogoutStringResources
import org.slf4j.Logger
import java.io.InputStream
import java.util.UUID

abstract class ProfileAccountLogoutTaskContract {

  private lateinit var account: AccountType
  private lateinit var accountID: AccountID
  private lateinit var adeptConnector: AdobeAdeptConnectorType
  private lateinit var adeptExecutor: AdobeAdeptExecutorType
  private lateinit var bookDatabase: BookDatabaseType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var http: LSHTTPClientType
  private lateinit var logoutStrings: AccountLogoutStringResourcesType
  private lateinit var patronParsers: PatronUserProfileParsersType
  private lateinit var profile: ProfileReadableType
  private lateinit var profileID: ProfileID
  private lateinit var server: MockWebServer

  private var loginState: AccountLoginState? = null

  abstract val logger: Logger

  @Before
  fun testSetup() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration("simplified-tests", "0.0.1")
        )

    this.profile =
      Mockito.mock(ProfileReadableType::class.java)
    this.account =
      Mockito.mock(AccountType::class.java)
    this.logoutStrings =
      MockAccountLogoutStringResources()
    this.patronParsers =
      Mockito.mock(PatronUserProfileParsersType::class.java)
    this.bookRegistry =
      Mockito.mock(BookRegistryType::class.java)
    this.bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    this.adeptConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)
    this.adeptExecutor =
      Mockito.mock(AdobeAdeptExecutorType::class.java)

    this.accountID =
      AccountID(UUID.randomUUID())
    this.profileID =
      ProfileID(UUID.randomUUID())

    this.server = MockWebServer()
    this.server.start()
  }

  @After
  fun testTearDown() {
    this.server.close()
  }

  /**
   * Logging out of an account that isn't logged in does nothing.
   */

  @Test
  fun testLogoutNotRequired() {
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
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }

    this.account.setLoginState(AccountNotLoggedIn)

    val task =
      ProfileAccountLogoutTask(
        account = this.account,
        adeptExecutor = null,
        bookRegistry = this.bookRegistry,
        http = this.http,
        patronParsers = PatronUserProfileParsers(),
        profile = this.profile,
        logoutStrings = this.logoutStrings
      )

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.resolution) }

    val state =
      this.account.loginState as AccountNotLoggedIn

    Mockito.verify(this.bookDatabase, Mockito.times(0))
      .delete()
  }

  /**
   * Logging out without DRM succeeds and does all the necessary cleanup.
   */

  @Test
  fun testLogoutNoDRM() {
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
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)

    val books =
      sortedSetOf(BookID.create("a"), BookID.create("b"), BookID.create("c"))

    Mockito.`when`(this.bookDatabase.books())
      .thenReturn(books)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    this.account.setLoginState(AccountLoggedIn(credentials))

    val task =
      ProfileAccountLogoutTask(
        account = this.account,
        adeptExecutor = null,
        bookRegistry = this.bookRegistry,
        http = this.http,
        patronParsers = PatronUserProfileParsers(),
        profile = this.profile,
        logoutStrings = this.logoutStrings
      )

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.resolution) }

    val state =
      this.account.loginState as AccountNotLoggedIn

    Mockito.verify(this.bookDatabase, Mockito.times(1))
      .delete()
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("a"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("b"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("c"))
  }

  /**
   * If the user logged in with DRM support, but is now logging out without DRM support,
   * logging out succeeds anyway.
   */

  @Test
  fun testLogoutDRMAdobeNoLongerSupported() {
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
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)

    val books =
      sortedSetOf(BookID.create("a"), BookID.create("b"), BookID.create("c"))

    Mockito.`when`(this.bookDatabase.books())
      .thenReturn(books)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("pass"),
        adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("patron").toUri(),
          postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
            deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
            userID = AdobeUserID("someone")
          )
        ),
        authenticationDescription = null
      )

    this.account.setLoginState(AccountLoggedIn(credentials))

    val task =
      ProfileAccountLogoutTask(
        account = this.account,
        adeptExecutor = null,
        bookRegistry = this.bookRegistry,
        http = this.http,
        patronParsers = PatronUserProfileParsers(),
        profile = this.profile,
        logoutStrings = this.logoutStrings
      )

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.resolution) }

    val state =
      this.account.loginState as AccountNotLoggedIn

    Mockito.verify(this.bookDatabase, Mockito.times(1))
      .delete()
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("a"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("b"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("c"))
  }

  /**
   * If the DRM connector raises an error, logging out fails. Credentials are preserved
   * so that it's possible to retry.
   */

  @Test
  fun testLogoutDRMAdobeError() {
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
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)

    val books =
      sortedSetOf(BookID.create("a"), BookID.create("b"), BookID.create("c"))

    Mockito.`when`(this.bookDatabase.books())
      .thenReturn(books)

    /*
     * When the code calls deactivateDevice(), it fails if the connector returns an error.
     */

    Mockito.`when`(
      this.adeptConnector.deactivateDevice(
        anyNonNull(),
        anyNonNull(),
        anyNonNull(),
        anyNonNull(),
        anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptDeactivationReceiverType
      receiver.onDeactivationError("E_FAIL_OFTEN_AND_LOUDLY")
    }

    Mockito.`when`(this.adeptExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("pass"),
        adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("patron").toUri(),
          postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
            deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
            userID = AdobeUserID("someone")
          )
        ),
        authenticationDescription = null
      )

    this.account.setLoginState(AccountLoggedIn(credentials))

    val task =
      ProfileAccountLogoutTask(
        account = this.account,
        adeptExecutor = this.adeptExecutor,
        bookRegistry = this.bookRegistry,
        http = this.http,
        patronParsers = PatronUserProfileParsers(),
        profile = this.profile,
        logoutStrings = this.logoutStrings
      )

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.resolution) }

    val state =
      this.account.loginState as AccountLogoutFailed

    Assert.assertEquals(credentials, state.credentials)

    Mockito.verify(this.bookDatabase, Mockito.times(0))
      .delete()
    Mockito.verify(this.bookRegistry, Mockito.times(0))
      .clearFor(anyNonNull())
  }

  /**
   * Logging out with DRM succeeds and does all the necessary cleanup, assuming that the connector
   * doesn't report any errors.
   */

  @Test
  fun testLogoutDRMAdobe() {
    val provider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(provider.patronSettingsURI)
      .thenReturn(this.server.url("patron").toUri())

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
    Mockito.`when`(this.account.setLoginState(anyNonNull()))
      .then {
        val newState = it.getArgument<AccountLoginState>(0)
        this.logger.debug("new state: {}", newState)
        this.loginState = newState
        this.loginState
      }
    Mockito.`when`(this.account.loginState)
      .then { this.loginState }
    Mockito.`when`(this.account.bookDatabase)
      .thenReturn(this.bookDatabase)

    val books =
      sortedSetOf(BookID.create("a"), BookID.create("b"), BookID.create("c"))

    Mockito.`when`(this.bookDatabase.books())
      .thenReturn(books)

    val patron =
      resource("/org/nypl/simplified/tests/patron/example-with-device.json")
        .readBytes()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(Buffer().write(patron))
    )

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("")
    )

    /*
     * When the code calls deactivateDevice(), it succeeds if the connector does not return an error.
     */

    Mockito.`when`(
      this.adeptConnector.deactivateDevice(
        anyNonNull(),
        anyNonNull(),
        anyNonNull(),
        anyNonNull(),
        anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptDeactivationReceiverType
      receiver.onDeactivationSucceeded()
    }

    Mockito.`when`(this.adeptExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adeptConnector)
      }

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("user"),
        password = AccountPassword("pass"),
        adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
          vendorID = AdobeVendorID("OmniConsumerProducts"),
          clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
          deviceManagerURI = this.server.url("patron").toUri(),
          postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
            deviceID = AdobeDeviceID("484799fb-d1aa-4b5d-8179-95e0b115ace4"),
            userID = AdobeUserID("someone")
          )
        ),
        authenticationDescription = null
      )

    this.account.setLoginState(AccountLoggedIn(credentials))

    val task =
      ProfileAccountLogoutTask(
        account = this.account,
        adeptExecutor = this.adeptExecutor,
        bookRegistry = this.bookRegistry,
        http = this.http,
        profile = this.profile,
        patronParsers = PatronUserProfileParsers(),
        logoutStrings = this.logoutStrings
      )

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.resolution) }

    val state =
      this.account.loginState as AccountNotLoggedIn

    Mockito.verify(this.bookDatabase, Mockito.times(1))
      .delete()
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("a"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("b"))
    Mockito.verify(this.bookRegistry, Mockito.times(1))
      .clearFor(BookID.create("c"))
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  private fun resource(
    name: String
  ): InputStream {
    return ProfileAccountLogoutTaskContract::class.java.getResourceAsStream(name)!!
  }
}
