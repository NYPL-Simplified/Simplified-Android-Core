package org.nypl.simplified.tests.books.profiles

import android.content.Context
import com.io7m.jfunctional.Option
import io.reactivex.subjects.PublishSubject
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.core.StringContains
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
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
import org.nypl.simplified.profiles.api.ProfileCreateDuplicateException
import org.nypl.simplified.profiles.api.ProfileDatabaseDeleteAnonymousException
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileNonexistentException
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.tests.MockAccountProviderRegistry
import org.nypl.simplified.tests.MockAccountProviders
import org.nypl.simplified.tests.books.accounts.FakeAccountCredentialStorage
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

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  protected abstract fun context(): Context

  @Before
  open fun setup() {
    this.credentialStore = FakeAccountCredentialStorage()
    this.accountEvents = PublishSubject.create()
    this.profileEvents = PublishSubject.create()
    this.analytics = Mockito.mock(AnalyticsType::class.java)
  }

  /**
   * An exception matcher that checks to see if the given profile database exception has
   * at least one cause of the given type and with the given exception message.
   *
   * @param <T> The cause type
   */

  private class CausesContains<T : Exception> internal constructor(
    private val exception_type: Class<T>,
    private val message: String
  ) : BaseMatcher<ProfileDatabaseException>() {

    private val logger = LoggerFactory.getLogger(CausesContains::class.java)

    override fun matches(item: Any): Boolean {
      if (item is ProfileDatabaseException) {
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
      description.appendText("must throw ProfileDatabaseException")
      description.appendText(" with at least one cause of type ${this.exception_type}")
      description.appendText(" with a message containing '${this.message}'")
    }
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

    this.expected.expect(ProfileDatabaseException::class.java)
    this.expected.expect(CausesContains(IOException::class.java, "Not a directory"))
    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      fileProfiles
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

    this.expected.expect(ProfileDatabaseException::class.java)
    this.expected.expect(CausesContains(IOException::class.java, "Not a directory"))
    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      fileProfiles
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

    this.expected.expect(ProfileDatabaseException::class.java)
    this.expected.expect(CausesContains(IOException::class.java, "Could not parse profile: "))
    ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      fileProfiles
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
      fileProfiles
    )

    Assert.assertEquals(0, db.profiles().size.toLong())
    Assert.assertEquals(fileProfiles, db.directory())
    Assert.assertTrue(db.currentProfile().isNone)
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
        fileProfiles
      )

    val accountProvider = accountProviderList[0]
    val p0 = db.createProfile(accountProvider, "Kermit")
    val p1 = db.createProfile(accountProvider, "Gonzo")
    val p2 = db.createProfile(accountProvider, "Beaker")

    Assert.assertFalse("Profile is not anonymous", p0.isAnonymous)
    Assert.assertFalse("Profile is not anonymous", p1.isAnonymous)
    Assert.assertFalse("Profile is not anonymous", p2.isAnonymous)

    Assert.assertEquals("Kermit", p0.displayName)
    Assert.assertEquals("Gonzo", p1.displayName)
    Assert.assertEquals("Beaker", p2.displayName)

    Assert.assertNotEquals(p0.id, p1.id)
    Assert.assertNotEquals(p0.id, p2.id)
    Assert.assertNotEquals(p1.id, p2.id)

    Assert.assertTrue(
      "Kermit profile exists",
      p0.directory.isDirectory
    )

    Assert.assertTrue(
      "Kermit profile file exists",
      File(p0.directory, "profile.json").isFile
    )

    Assert.assertTrue(
      "Gonzo profile exists",
      p1.directory.isDirectory
    )

    Assert.assertTrue(
      "Gonzo profile file exists",
      File(p1.directory, "profile.json").isFile
    )

    Assert.assertTrue(
      "Beaker profile exists",
      p1.directory.isDirectory
    )

    Assert.assertTrue(
      "Beaker profile file exists",
      File(p2.directory, "profile.json").isFile
    )

    Assert.assertFalse(p0.isCurrent)
    Assert.assertFalse(p1.isCurrent)
    Assert.assertFalse(p2.isCurrent)
    Assert.assertTrue(db.currentProfile().isNone)
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
      fileProfiles
    )

    val pr0 = db1.profiles()[p0.id]!!
    val pr1 = db1.profiles()[p1.id]!!
    val pr2 = db1.profiles()[p2.id]!!

    Assert.assertEquals(p0.directory, pr0.directory)
    Assert.assertEquals(p1.directory, pr1.directory)
    Assert.assertEquals(p2.directory, pr2.directory)

    Assert.assertEquals(p0.displayName, pr0.displayName)
    Assert.assertEquals(p1.displayName, pr1.displayName)
    Assert.assertEquals(p2.displayName, pr2.displayName)

    Assert.assertEquals(p0.id, pr0.id)
    Assert.assertEquals(p1.id, pr1.id)
    Assert.assertEquals(p2.id, pr2.id)
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

    Assert.assertEquals(
      ProfileDateOfBirth(DateTime(20L), true),
      p0.preferences().dateOfBirth
    )
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
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db.createProfile(acc, "Kermit")

    this.expected.expect(ProfileDatabaseException::class.java)
    this.expected.expectMessage(StringContains.containsString("Display name is already used"))
    db.createProfile(acc, "Kermit")
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
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    this.expected.expect(ProfileDatabaseException::class.java)
    this.expected.expectMessage(StringContains.containsString("Display name cannot be empty"))
    db.createProfile(acc, "")
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
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")

    db0.setProfileCurrent(p0.id)

    Assert.assertTrue(p0.isCurrent)
    Assert.assertEquals(Option.some(p0), db0.currentProfile())
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
      fileProfiles
    )

    val acc = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")

    this.expected.expect(ProfileNonexistentException::class.java)
    this.expected.expectMessage(StringContains.containsString("Profile does not exist"))
    db0.setProfileCurrent(ProfileID(UUID.fromString("135dec78-b89b-4a6c-bf6a-294c1694d40b")))
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
        fileProfiles
      )

    Assert.assertEquals(1L, db0.profiles().size.toLong())

    val p0 = db0.anonymousProfile()
    Assert.assertTrue("Anonymous profile must be enabled", p0.isAnonymous)
    Assert.assertEquals(Option.some(p0), db0.currentProfile())
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
        fileProfiles
      )

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileAnonymousEnabledException::class.java)
    db0.setProfileCurrent(ProfileID.generate())
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
        fileProfiles
      )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    val a0 = p0.createAccount(acc1)
    Assert.assertTrue("Account must exist", p0.accounts().containsKey(a0.id))
    p0.deleteAccountByProvider(acc1.id)
    Assert.assertFalse("Account must not exist", p0.accounts().containsKey(a0.id))
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
      fileProfiles
    )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    this.expected.expect(AccountsDatabaseNonexistentException::class.java)
    p0.deleteAccountByProvider(acc1.id)
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
      fileProfiles
    )

    val acc0 =
      MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id)

    this.expected.expect(AccountsDatabaseLastAccountException::class.java)
    p0.deleteAccountByProvider(acc0.id)
  }

  /**
   * If the deleted account was the most recent account, the most recent account preference
   * is cleared.
   */

  @Test
  @Throws(Exception::class)
  fun testDeleteClearsMostRecent() {
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

    Assert.assertEquals(acci1.id, p0.preferences().mostRecentAccount)
    p0.deleteAccountByProvider(acc1.id)
    Assert.assertEquals(null, p0.preferences().mostRecentAccount)
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
      fileProfiles
    )

    this.expected.expect(ProfileAnonymousDisabledException::class.java)
    this.expected.expectMessage(StringContains.containsString("The anonymous profile is not enabled"))
    db0.anonymousProfile()
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
        fileProfiles
      )

    val p0After = db1.profiles()[db1.profiles().firstKey()]!!
    Assert.assertEquals(1, p0After.accounts().size)
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
        fileProfiles
      )

    val p1 = db1.anonymousProfile()

    Assert.assertTrue(p0.accounts().containsKey(acc0.id))
    Assert.assertTrue(p0.accounts().containsKey(acc0.id))
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

    Assert.assertEquals(2, accountsByProvider.size)
    Assert.assertNotNull(accountsByProvider[acc.id])
    Assert.assertNotNull(accountsByProvider[MockAccountProviders.fakeAccountProviderDefaultAutoURI()])
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
      f_pro
    )

    val p0 = db0.currentProfileUnsafe()
    this.expected.expect(ProfileDatabaseDeleteAnonymousException::class.java)
    p0.delete()
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
      f_pro
    )

    val p0 = db0.createProfile(acc0, "Kermit")
    Assert.assertEquals(Option.none<ProfileType>(), db0.currentProfile())
    db0.setProfileCurrent(p0.id)
    Assert.assertEquals(Option.some(p0), db0.currentProfile())

    p0.delete()
    Assert.assertEquals(Option.none<ProfileType>(), db0.currentProfile())

    val db1 = ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analytics,
      this.accountEvents,
      accountProviders,
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro
    )

    Assert.assertEquals(0L, db1.profiles().size.toLong())
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
      f_pro
    )

    val acc0 = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 = MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    val p1 = db0.createProfile(acc0, "Grouch")

    this.expected.expect(ProfileCreateDuplicateException::class.java)
    p0.setDescription(p0.description().copy(displayName = "Grouch"))
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
      f_pro
    )

    val acc0 = MockAccountProviders.fakeProvider("http://www.example.com/accounts0/")
    val acc1 = MockAccountProviders.fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    val p1 = db0.createProfile(acc0, "Grouch")

    p0.setDescription(p0.description().copy(displayName = "Big Bird"))

    Assert.assertEquals("Big Bird", p0.displayName)
    Assert.assertEquals("Grouch", p1.displayName)
  }
}
