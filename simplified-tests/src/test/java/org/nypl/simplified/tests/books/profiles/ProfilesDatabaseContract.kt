package org.nypl.simplified.tests.books.profiles

import android.content.Context
import com.io7m.jfunctional.Option
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseLastAccountException
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileAnonymousDisabledException
import org.nypl.simplified.profiles.api.ProfileAnonymousEnabledException
import org.nypl.simplified.profiles.api.ProfileCreateDuplicateException
import org.nypl.simplified.profiles.api.ProfileDatabaseDeleteAnonymousException
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.tests.books.BookFormatsTesting
import org.nypl.simplified.tests.mocking.FakeAccountCredentialStorage
import org.nypl.simplified.tests.mocking.MockAccountProviderRegistry
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID

abstract class ProfilesDatabaseContract {

  private val logger = LoggerFactory.getLogger(ProfilesDatabaseContract::class.java)

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var analytics: AnalyticsType
  private lateinit var credentialStore: FakeAccountCredentialStorage
  private lateinit var profileEvents: PublishSubject<ProfileEvent>

  protected abstract fun context(): Context

  @BeforeEach
  open fun setup() {
    this.credentialStore = FakeAccountCredentialStorage()
    this.accountEvents = PublishSubject.create()
    this.profileEvents = PublishSubject.create()
    this.analytics = Mockito.mock(AnalyticsType::class.java)
  }

  private fun onAccountResolution(
    id: URI,
    message: String
  ) {
    this.logger.debug("resolution: {}: {}", id, message)
  }

  /**
   * If the profile directory is not a directory, opening it fails.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingNotDirectory() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")
    FileUtilities.fileWriteUTF8(fileProfiles, "Hello!")

    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        ProfilesDatabases.openWithAnonymousProfileDisabled(
          this.context(),
          this.analytics,
          this.accountEvents,
          MockAccountProviders.fakeAccountProviders(),
          AccountBundledCredentialsEmpty.getInstance(),
          this.credentialStore,
          this.accountsDatabases(),
          BookFormatsTesting.supportsEverything,
          fileProfiles
        )
      }
    )

    Assertions.assertTrue(
      ex.causes()
        .find { e -> e is IOException && e.message!!.contains("Not a directory") } != null
    )
  }

  /**
   * A subdirectory that can't be parsed as a UUID will be migrated.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingBadSubdirectory() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    fileProfiles.mkdirs()
    val f_bad = File(fileProfiles, "not-a-number")
    f_bad.mkdirs()

    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )
  }

  /**
   * A subdirectory that isn't a file causes a failure.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingFileSubdirectory() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    fileProfiles.mkdirs()
    val f_bad = File(fileProfiles, UUID.randomUUID().toString())
    f_bad.writeText("Not a profile, clearly.")

    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        ProfilesDatabases.openWithAnonymousProfileDisabled(
          this.context(),
          this.analytics,
          this.accountEvents,
          MockAccountProviders.fakeAccountProviders(),
          AccountBundledCredentialsEmpty.getInstance(),
          this.credentialStore,
          this.accountsDatabases(),
          BookFormatsTesting.supportsEverything,
          fileProfiles
        )
      }
    )

    Assertions.assertTrue(
      ex.causes()
        .find { e -> e is IOException && e.message!!.contains("Not a directory") } != null
    )
  }

  private fun accountsDatabases(): AccountsDatabaseFactoryType {
    return org.nypl.simplified.accounts.database.AccountsDatabases
  }

  /**
   * A profile with a missing metadata file is ignored.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONMissing() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    fileProfiles.mkdirs()
    val f_0 = File(fileProfiles, "0")
    f_0.mkdirs()

    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )
  }

  /**
   * A profile with a broken metadata file causes an exception.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONUnparseable() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    fileProfiles.mkdirs()
    val f_0 = File(fileProfiles, "0")
    f_0.mkdirs()
    val f_p = File(f_0, "profile.json")
    FileUtilities.fileWriteUTF8(f_p, "} { this is not JSON { } { }")

    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        ProfilesDatabases.openWithAnonymousProfileDisabled(
          this.context(),
          this.analytics,
          this.accountEvents,
          MockAccountProviders.fakeAccountProviders(),
          AccountBundledCredentialsEmpty.getInstance(),
          this.credentialStore,
          this.accountsDatabases(),
          BookFormatsTesting.supportsEverything,
          fileProfiles
        )
      }
    )

    Assertions.assertTrue(
      ex.causes()
        .find { e -> e is IOException && e.message!!.contains("Could not parse profile: ") } != null
    )
  }

  /**
   * An empty set of profiles is empty!
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingEmpty() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    Assertions.assertEquals(0, db.profiles().size.toLong())
    Assertions.assertEquals(fileProfiles, db.directory())
    Assertions.assertTrue(db.currentProfile().isNone)
  }

  /**
   * Creating profiles works.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenCreateProfiles() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviderList =
      MockAccountProviders.fakeAccountProviderList()
    val accountProviders =
      MockAccountProviderRegistry.withProviders(accountProviderList)

    val db =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val accountProvider = accountProviderList[0]
    val p0 = db.createProfile(accountProvider, "Kermit")
    val p1 = db.createProfile(accountProvider, "Gonzo")
    val p2 = db.createProfile(accountProvider, "Beaker")

    Assertions.assertFalse(p0.isAnonymous, "Profile is not anonymous")
    Assertions.assertFalse(p1.isAnonymous, "Profile is not anonymous")
    Assertions.assertFalse(p2.isAnonymous, "Profile is not anonymous")

    Assertions.assertEquals("Kermit", p0.displayName)
    Assertions.assertEquals("Gonzo", p1.displayName)
    Assertions.assertEquals("Beaker", p2.displayName)

    Assertions.assertNotEquals(p0.id, p1.id)
    Assertions.assertNotEquals(p0.id, p2.id)
    Assertions.assertNotEquals(p1.id, p2.id)

    Assertions.assertTrue(p0.directory.isDirectory, "Kermit profile exists")
    Assertions.assertTrue(File(p0.directory, "profile.json").isFile, "Kermit profile file exists")
    Assertions.assertTrue(p1.directory.isDirectory, "Gonzo profile exists")
    Assertions.assertTrue(File(p1.directory, "profile.json").isFile, "Gonzo profile file exists")
    Assertions.assertTrue(p1.directory.isDirectory, "Beaker profile exists")
    Assertions.assertTrue(File(p2.directory, "profile.json").isFile, "Beaker profile file exists")
    Assertions.assertFalse(p0.isCurrent)
    Assertions.assertFalse(p1.isCurrent)
    Assertions.assertFalse(p2.isCurrent)
    Assertions.assertTrue(db.currentProfile().isNone)
  }

  /**
   * Creating profiles and then reopening the database shows the same profiles.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenCreateReopen() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc =
      MockAccountProviders.fakeProvider("urn:fake:0")

    val p0 = db0.createProfile(acc, "Kermit")
    val p1 = db0.createProfile(acc, "Gonzo")
    val p2 = db0.createProfile(acc, "Beaker")

    val db1 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val pr0 = db1.profiles()[p0.id]!!
    val pr1 = db1.profiles()[p1.id]!!
    val pr2 = db1.profiles()[p2.id]!!

    Assertions.assertEquals(p0.directory, pr0.directory)
    Assertions.assertEquals(p1.directory, pr1.directory)
    Assertions.assertEquals(p2.directory, pr2.directory)

    Assertions.assertEquals(p0.displayName, pr0.displayName)
    Assertions.assertEquals(p1.displayName, pr1.displayName)
    Assertions.assertEquals(p2.displayName, pr2.displayName)

    Assertions.assertEquals(p0.id, pr0.id)
    Assertions.assertEquals(p1.id, pr1.id)
    Assertions.assertEquals(p2.id, pr2.id)
  }

  /**
   * Updating preferences works.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenCreateUpdatePreferences() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db.createProfile(acc, "Kermit")
    p0.setDescription(
      ProfileDescription(
        p0.displayName,
        p0.preferences().copy(dateOfBirth = ProfileDateOfBirth(DateTime(20L), true)),
        p0.attributes()
      )
    )

    Assertions.assertEquals(ProfileDateOfBirth(DateTime(20L), true), p0.preferences().dateOfBirth)
  }

  /**
   * Creating duplicate profiles fails.
   */

  @Test
  @Throws(Exception::class)
  fun testCreateProfileDuplicate() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val p0 = db.createProfile(acc, "Kermit")
    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        db.createProfile(acc, "Kermit")
      }
    )
    Assertions.assertTrue(ex.message!!.contains("Display name is already used"))
  }

  /**
   * Creating a profile with an empty display name fails.
   */

  @Test
  @Throws(Exception::class)
  fun testCreateProfileEmptyDisplayName() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        db.createProfile(acc, "")
      }
    )
    Assertions.assertTrue(ex.message!!.contains("Display name cannot be empty"))
  }

  /**
   * Setting a profile to current works.
   */

  @Test
  @Throws(Exception::class)
  fun testSetCurrent() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")

    db0.setProfileCurrent(p0.id)

    Assertions.assertTrue(p0.isCurrent)
    Assertions.assertEquals(Option.some(p0), db0.currentProfile())
  }

  /**
   * Setting a nonexistent profile to current fails.
   */

  @Test
  @Throws(Exception::class)
  fun testSetCurrentNonexistent() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val p0 = db0.createProfile(acc, "Kermit")
    val ex = Assertions.assertThrows(
      ProfileDatabaseException::class.java,
      Executable {
        db0.setProfileCurrent(ProfileID(UUID.fromString("135dec78-b89b-4a6c-bf6a-294c1694d40b")))
      }
    )
    Assertions.assertTrue(ex.message!!.contains("Profile does not exist"))
  }

  /**
   * Opening a profile database in anonymous mode works.
   */

  @Test
  @Throws(Exception::class)
  fun testAnonymous() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileEnabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    Assertions.assertEquals(1L, db0.profiles().size.toLong())

    val p0 = db0.anonymousProfile()
    Assertions.assertTrue(p0.isAnonymous, "Anonymous profile must be enabled")
    Assertions.assertEquals(Option.some(p0), db0.currentProfile())
  }

  /**
   * Setting a profile to current in anonymous mode fails.
   */

  @Test
  @Throws(Exception::class)
  fun testAnonymousSetCurrent() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileEnabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    Assertions.assertThrows(
      ProfileAnonymousEnabledException::class.java,
      Executable {
        db0.setProfileCurrent(ProfileID.generate())
      }
    )
  }

  /**
   * Creating and deleting a profile works.
   */

  @Test
  @Throws(Exception::class)
  fun testCreateDelete() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        MockAccountProviders.fakeAccountProviders(),
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    val a0 = p0.createAccount(acc1)
    Assertions.assertTrue(p0.accounts().containsKey(a0.id), "Account must exist")
    p0.deleteAccountByProvider(acc1.id)
    Assertions.assertFalse(p0.accounts().containsKey(a0.id), "Account must not exist")
  }

  /**
   * Trying to delete an account with an unknown provider fails.
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteUnknownProvider() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    Assertions.assertThrows(
      AccountsDatabaseNonexistentException::class.java,
      Executable {
        p0.deleteAccountByProvider(acc1.id)
      }
    )
  }

  /**
   * Trying to delete the last account fails.
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteLastAccount() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    Assertions.assertThrows(
      AccountsDatabaseLastAccountException::class.java,
      Executable {
        p0.deleteAccountByProvider(acc0.id)
      }
    )
  }

  /**
   * If the deleted account was the most recent account, the most recent account preference
   * is updated.
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteUpdateMostRecent() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    val acci1 = p0.createAccount(acc1)
    p0.setDescription(
      p0.description().copy(
        preferences = p0.preferences().copy(
          mostRecentAccount = acci1.id
        )
      )
    )

    Assertions.assertEquals(acci1.id, p0.preferences().mostRecentAccount)
    p0.deleteAccountByProvider(acc1.id)
    Assertions.assertNotEquals(acci1.id, p0.preferences().mostRecentAccount)
  }

  /**
   * Trying to fetch the anonymous profile outside of anonymous mode, fails.
   */

  @Test
  @Throws(Exception::class)
  fun testAnonymousNotEnabled() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )

    val ex = Assertions.assertThrows(
      ProfileAnonymousDisabledException::class.java,
      Executable {
        db0.anonymousProfile()
      }
    )
    Assertions.assertTrue(ex.message!!.contains("The anonymous profile is not enabled"))
  }

  /**
   * If an account provider disappears, the profile database opens but the missing account
   * is not present.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testOpenCreateReopenMissingAccountProvider() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviderList =
      MockAccountProviders.fakeAccountProviderList()
    val accountProviders =
      MockAccountProviderRegistry.withProviders(accountProviderList)

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val accountProvider0 =
      accountProviderList[0]
    val accountProvider1 =
      accountProviderList[1]

    val p0 = db0.createProfile(accountProvider0, "Kermit")
    p0.createAccount(accountProvider1)

    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviderRegistry.singleton(accountProvider0),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      fileProfiles
    )
  }

  /**
   * If an account provider disappears, and the profile only contained a single account that
   * has now disappeared, a new account is created.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testOpenCreateReopenMissingAccountProviderNew() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviderList =
      MockAccountProviders.fakeAccountProviderList()
    val accountProviders =
      MockAccountProviderRegistry.withProviders(accountProviderList)
    val accountProvidersOnly1 =
      MockAccountProviderRegistry.singleton(accountProviderList[1])

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val p0 =
      db0.createProfile(accountProviderList[0], "Kermit")

    val db1 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProvidersOnly1,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val p0After = db1.profiles()[db1.profiles().firstKey()]!!
    Assertions.assertEquals(1, p0After.accounts().size)
  }

  /**
   * Repeatedly reopening a database in anonymous/non-anonymous mode doesn't cause any damage.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testOpenAnonymousNonAnonymousAlternating() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val acc =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val p0 =
      db0.createProfile(acc, "Kermit")
    val acc0 =
      p0.createAccount(MockAccountProviders.fakeProvider("urn:fake:1"))

    val db1 =
      ProfilesDatabases.openWithAnonymousProfileEnabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val p1 = db1.anonymousProfile()

    Assertions.assertTrue(p0.accounts().containsKey(acc0.id))
    Assertions.assertTrue(p0.accounts().containsKey(acc0.id))
  }

  /**
   * Automatic accounts are resolved and added.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenAutomatic() {
    val fileTemp = DirectoryUtilities.directoryCreateTemporary()
    val fileProfiles = File(fileTemp, "profiles")

    val accountProviders =
      MockAccountProviderRegistry.withProviders(
        MockAccountProviders.fakeAccountProviderListWithAutomatic()
      )

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        fileProfiles
      )

    val acc =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val p0 =
      db0.createProfile(acc, "Kermit")
    val accountsDatabase =
      p0.accountsDatabase()
    val accountsByProvider =
      accountsDatabase.accountsByProvider()

    Assertions.assertEquals(2, accountsByProvider.size)
    Assertions.assertNotNull(accountsByProvider[acc.id])
    Assertions.assertNotNull(accountsByProvider[MockAccountProviders.fakeAccountProviderDefaultAutoURI()])
  }

  /**
   * Deleting a profile works.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteAnonymous() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 = ProfilesDatabases.openWithAnonymousProfileEnabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      f_pro
    )

    val p0 = db0.currentProfileUnsafe()
    Assertions.assertThrows(
      ProfileDatabaseDeleteAnonymousException::class.java,
      Executable {
        p0.delete()
      }
    )
  }

  /**
   * Deleting a profile works.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteOK() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      f_pro
    )

    val p0 = db0.createProfile(acc0, "Kermit")
    Assertions.assertEquals(Option.none<ProfileType>(), db0.currentProfile())
    db0.setProfileCurrent(p0.id)
    Assertions.assertEquals(Option.some(p0), db0.currentProfile())

    p0.delete()
    Assertions.assertEquals(Option.none<ProfileType>(), db0.currentProfile())

    val db1 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      f_pro
    )

    Assertions.assertEquals(0L, db1.profiles().size.toLong())
  }

  /**
   * Renaming a profile fails if another profile has the same name.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testCreateRenameDuplicate() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      f_pro
    )

    val acc0 = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 = MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    val p1 = db0.createProfile(acc0, "Grouch")

    Assertions.assertThrows(
      ProfileCreateDuplicateException::class.java,
      Executable {
        p0.setDescription(p0.description().copy(displayName = "Grouch"))
      }
    )
  }

  /**
   * Renaming a profile succeeds if no other profile has the name.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testCreateRenameOK() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      BookFormatsTesting.supportsEverything,
      f_pro
    )

    val acc0 = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 = MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    val p1 = db0.createProfile(acc0, "Grouch")

    p0.setDescription(p0.description().copy(displayName = "Big Bird"))

    Assertions.assertEquals("Big Bird", p0.displayName)
    Assertions.assertEquals("Grouch", p1.displayName)
  }

  /**
   * If the "most recent account" ID refers to an account that doesn't exist, then it must
   * be wiped out when the profiles database is opened.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testInvalidMostRecent() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val accountProviders =
      MockAccountProviders.fakeAccountProviders()

    val db0 =
      ProfilesDatabases.openWithAnonymousProfileEnabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        f_pro
      )

    val pro0 = db0.currentProfileUnsafe()
    val acc0 = pro0.mostRecentAccount()

    val acc1p = MockAccountProviders.fakeProvider("urn:fake:1")
    val acc1 = pro0.createAccount(acc1p)

    val pro0desc =
      pro0.description()
    val pro0descNew =
      pro0desc.copy(preferences = pro0desc.preferences.copy(mostRecentAccount = acc1.id))

    pro0.setDescription(pro0descNew)

    /*
     * Delete the account on disk without going through the proper channels.
     */

    val f_account =
      File(File(File(f_pro, pro0.id.uuid.toString()), "accounts"), acc1.id.uuid.toString())

    this.logger.debug("deleting account {}", acc1.id.uuid)
    f_account.deleteRecursively()

    val db1 =
      ProfilesDatabases.openWithAnonymousProfileEnabled(
        this.context(),
        this.analytics,
        this.accountEvents,
        accountProviders,
        AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        this.accountsDatabases(),
        BookFormatsTesting.supportsEverything,
        f_pro
      )

    val pro1 = db1.currentProfileUnsafe()
    pro1.account(pro1.preferences().mostRecentAccount)

    Assertions.assertEquals(acc0.id, pro1.preferences().mostRecentAccount)
  }
}
