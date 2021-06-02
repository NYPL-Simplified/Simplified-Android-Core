package org.nypl.simplified.tests.books.accounts

import android.content.Context
import io.reactivex.subjects.PublishSubject
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.AccountsDatabase
import org.nypl.simplified.accounts.database.api.AccountsDatabaseDuplicateProviderException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.book_database.BookDatabases
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.tests.mocking.FakeAccountCredentialStorage
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI

abstract class AccountsDatabaseContract {

  private val logger = LoggerFactory.getLogger(AccountsDatabaseContract::class.java)

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountProviders: AccountProviderRegistryType
  private lateinit var credentialStore: FakeAccountCredentialStorage
  private lateinit var profileEvents: PublishSubject<ProfileEvent>

  protected abstract fun context(): Context

  @BeforeEach
  open fun setup() {
    this.credentialStore = FakeAccountCredentialStorage()
    this.accountEvents = PublishSubject.create()
    this.profileEvents = PublishSubject.create()
    this.accountProviders = Mockito.mock(AccountProviderRegistryType::class.java)
  }

  /**
   * An exception matcher that checks to see if the given accounts database exception has
   * at least one cause of the given type and with the given exception message.
   *
   * @param <T> The cause type
   */

  private class CausesContains<T : Exception>(
    private val exception_type: Class<T>,
    private val message: String
  ) : BaseMatcher<AccountsDatabaseException>() {

    private val logger = LoggerFactory.getLogger(CausesContains::class.java)

    override fun matches(item: Any): Boolean {
      if (item is AccountsDatabaseException) {
        for (c in item.causes()) {
          this.logger.error("Cause: ", c)
          if (this.exception_type.isAssignableFrom(c.javaClass) && c.message!!.contains(this.message)) {
            return true
          }
        }
      }
      return false
    }

    override fun describeTo(description: Description) {
      description.appendText("must throw AccountsDatabaseException")
      description.appendText(" with at least one cause of type ${this.exception_type}")
      description.appendText(" with a message containing '${this.message}'")
    }
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingNotDirectory() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    FileUtilities.fileWriteUTF8(f_acc, "Hello!")

    val ex = Assertions.assertThrows(AccountsDatabaseException::class.java) {
      AccountsDatabase.open(
        this.context(),
        this.accountEvents,
        this.bookDatabases(),
        this.credentialStore,
        this.accountProviders,
        f_acc
      )
    }

    val causes =
      ex.causes()
        .filterIsInstance<IOException>()
        .find { ex -> ex.message!!.contains("Not a directory") }
    Assertions.assertTrue(causes != null)
  }

  private fun bookDatabases(): BookDatabases {
    return BookDatabases
  }

  private fun onAccountResolutionStatus(
    accountProvider: URI,
    message: String
  ) {
    this.logger.debug("onAccountResolutionStatus: {}: {}", accountProvider, message)
  }

  /**
   * A bad subdirectory will be migrated but will still fail to open.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingBadSubdirectory() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")
    f_acc.mkdirs()
    val f_a = File(f_acc, "xyz")
    f_a.mkdirs()

    val ex = Assertions.assertThrows(AccountsDatabaseException::class.java) {
      val database = AccountsDatabase.open(
        this.context(),
        this.accountEvents,
        this.bookDatabases(),
        this.credentialStore,
        this.accountProviders,
        f_acc
      )
    }

    val causes =
      ex.causes()
        .filterIsInstance<IOException>()
        .find { ex -> ex.message!!.contains("Could not parse account: ") }
    Assertions.assertTrue(causes != null)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONMissing() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")
    f_acc.mkdirs()
    val f_a = File(f_acc, "64e606e8-e242-4c5f-9bfb-63df11b187bd")
    f_a.mkdirs()

    val ex = Assertions.assertThrows(AccountsDatabaseException::class.java) {
      AccountsDatabase.open(
        this.context(),
        this.accountEvents,
        this.bookDatabases(),
        this.credentialStore,
        this.accountProviders,
        f_acc
      )
    }

    val causes =
      ex.causes()
        .filterIsInstance<IOException>()
        .find { ex -> ex.message!!.contains("Could not parse account: ") }
    Assertions.assertTrue(causes != null)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONUnparseable() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")
    f_acc.mkdirs()
    val f_a = File(f_acc, "0")
    f_a.mkdirs()

    val f_f = File(f_a, "account.json")
    FileUtilities.fileWriteUTF8(f_f, "} { this is not JSON { } { }")

    val ex = Assertions.assertThrows(AccountsDatabaseException::class.java) {
      AccountsDatabase.open(
        this.context(),
        this.accountEvents,
        this.bookDatabases(),
        this.credentialStore,
        this.accountProviders,
        f_acc
      )
    }

    val causes =
      ex.causes()
        .filterIsInstance<IOException>()
        .find { ex -> ex.message!!.contains("Could not parse account: ") }
    Assertions.assertTrue(causes != null)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingEmpty() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    Assertions.assertEquals(0, db.accounts().size.toLong())
    Assertions.assertEquals(f_acc, db.directory())
  }

  @Test
  @Throws(Exception::class)
  fun testCreateAccount() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val provider0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val provider1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val acc0 = db.createAccount(provider0)
    val acc1 = db.createAccount(provider1)

    Assertions.assertTrue(acc0.directory.isDirectory, "Account 0 directory exists")
    Assertions.assertTrue(acc1.directory.isDirectory, "Account 1 directory exists")
    Assertions.assertTrue(File(acc0.directory, "account.json").isFile, "Account 0 file exists")
    Assertions.assertTrue(File(acc1.directory, "account.json").isFile, "Account 1 file exists")

    Assertions.assertEquals(provider0, acc0.provider)
    Assertions.assertEquals(provider1, acc1.provider)

    Assertions.assertNotEquals(acc0.id, acc1.id)
    Assertions.assertNotEquals(acc0.directory, acc1.directory)

    Assertions.assertEquals(AccountLoginState.AccountNotLoggedIn, acc0.loginState)
    Assertions.assertEquals(AccountLoginState.AccountNotLoggedIn, acc1.loginState)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateAccountProviderAlreadyUsed() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db =
      AccountsDatabase.open(
        this.context(),
        this.accountEvents,
        this.bookDatabases(),
        this.credentialStore,
        this.accountProviders,
        f_acc
      )

    val provider0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val acc0 = db.createAccount(provider0)

    Assertions.assertThrows(AccountsDatabaseDuplicateProviderException::class.java) {
      val acc1 = db.createAccount(provider0)
    }
  }

  @Test
  @Throws(Exception::class)
  fun testCreateReopen() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db0 = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val provider0 =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val provider1 =
      MockAccountProviders.fakeProvider("urn:fake:1")

    val acc0 = db0.createAccount(provider0)
    val acc1 = db0.createAccount(provider1)

    val db1 = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val acr0 = db1.accounts()[acc0.id]!!
    val acr1 = db1.accounts()[acc1.id]!!

    Assertions.assertEquals(acc0.id, acr0.id)
    Assertions.assertEquals(acc1.id, acr1.id)
    Assertions.assertEquals(acc0.directory, acr0.directory)
    Assertions.assertEquals(acc1.directory, acr1.directory)
    Assertions.assertEquals(acc0.provider.id, acr0.provider.id)
    Assertions.assertEquals(acc1.provider.id, acr1.provider.id)
  }

  @Test
  @Throws(Exception::class)
  fun testSetCredentials() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db0 = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val provider0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc0 = db0.createAccount(provider0)

    val creds =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    acc0.setLoginState(AccountLoginState.AccountLoggedIn(creds))
    Assertions.assertEquals(AccountLoginState.AccountLoggedIn(creds), acc0.loginState)

    acc0.updateCredentialsIfAvailable {
      when (it) {
        is AccountAuthenticationCredentials.Basic ->
          creds.copy(annotationsURI = URI.create("https://www.example.com/annotations"))
        is AccountAuthenticationCredentials.OAuthWithIntermediary ->
          creds.copy(annotationsURI = URI.create("https://www.example.com/annotations"))
        is AccountAuthenticationCredentials.SAML2_0 ->
          creds.copy(annotationsURI = URI.create("https://www.example.com/annotations"))
      }
    }

    Assertions.assertEquals(
      URI.create("https://www.example.com/annotations"),
      acc0.loginState.credentials?.annotationsURI
    )
  }

  @Test
  @Throws(Exception::class)
  fun testSetProviderWrongID() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db0 = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val provider0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val provider1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val acc0 = db0.createAccount(provider0)

    Assertions.assertThrows(AccountsDatabaseException::class.java) {
      acc0.setAccountProvider(provider1)
    }
  }

  @Test
  @Throws(Exception::class)
  fun testSetProviderOK() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    fileProfiles.mkdirs()
    val f_p = File(fileProfiles, "0")
    f_p.mkdirs()
    val f_acc = File(f_p, "accounts")

    val db0 = AccountsDatabase.open(
      this.context(),
      this.accountEvents,
      this.bookDatabases(),
      this.credentialStore,
      this.accountProviders,
      f_acc
    )

    val provider0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val provider1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val acc0 = db0.createAccount(provider0)
    acc0.setAccountProvider(provider1)
  }
}
