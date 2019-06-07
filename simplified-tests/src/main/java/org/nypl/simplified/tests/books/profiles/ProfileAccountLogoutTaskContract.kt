package org.nypl.simplified.tests.books.profiles

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.ProfileAccountLogoutTask
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.tests.MockAccountLogoutStringResources
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.util.UUID

abstract class ProfileAccountLogoutTaskContract {

  private lateinit var bookDatabase: BookDatabaseType
  private lateinit var profileID: ProfileID
  private lateinit var accountID: AccountID
  private lateinit var bookRegistry: BookRegistryType
  private var loginState: AccountLoginState? = null
  private lateinit var patronParsers: PatronUserProfileParsersType
  private lateinit var logoutStrings: AccountLogoutStringResourcesType
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
    this.logoutStrings =
      MockAccountLogoutStringResources()
    this.patronParsers =
      Mockito.mock(PatronUserProfileParsersType::class.java)
    this.bookRegistry =
      Mockito.mock(BookRegistryType::class.java)
    this.bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)

    this.accountID =
      AccountID(UUID.randomUUID())
    this.profileID =
      ProfileID(UUID.randomUUID())
  }

  @After
  fun testTearDown() {

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

    this.account.setLoginState(AccountNotLoggedIn)

    val task =
      ProfileAccountLogoutTask(
        profile = this.profile,
        account = this.account,
        accountLogoutStrings = this.logoutStrings,
        bookRegistry = this.bookRegistry)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountNotLoggedIn

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
    Mockito.`when`(this.account.bookDatabase())
      .thenReturn(this.bookDatabase)

    val books =
      sortedSetOf(BookID.create("a"), BookID.create("b"), BookID.create("c"))

    Mockito.`when`(this.bookDatabase.books())
      .thenReturn(books)

    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("pin"),
        AccountBarcode.create("barcode"))
        .build()

    this.account.setLoginState(AccountLoggedIn(credentials))

    val task =
      ProfileAccountLogoutTask(
        profile = this.profile,
        account = this.account,
        accountLogoutStrings = this.logoutStrings,
        bookRegistry = this.bookRegistry)

    val result = task.call()
    this.logger.debug("result: {}", result)
    result.steps.forEach { step -> this.logger.debug("step {}: {}", step, step.exception) }

    val state =
      this.account.loginState() as AccountNotLoggedIn

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
}