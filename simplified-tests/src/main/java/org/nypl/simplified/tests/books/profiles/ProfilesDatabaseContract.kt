package org.nypl.simplified.tests.books.profiles

import android.content.Context
import com.io7m.jfunctional.Option
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.core.StringContains
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountProviders
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.tests.books.accounts.FakeAccountCredentialStorage
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.TreeMap
import java.util.UUID

abstract class ProfilesDatabaseContract {

  private lateinit var credentialStore: FakeAccountCredentialStorage
  private lateinit var accountEvents: ObservableType<org.nypl.simplified.accounts.api.AccountEvent>
  private lateinit var profileEvents: ObservableType<org.nypl.simplified.profiles.api.ProfileEvent>

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  protected abstract fun context(): Context

  @Before
  open fun setup() {
    this.credentialStore = FakeAccountCredentialStorage()
    this.accountEvents = Observable.create()
    this.profileEvents = Observable.create()
  }

  /**
   * An exception matcher that checks to see if the given profile database exception has
   * at least one cause of the given type and with the given exception message.
   *
   * @param <T> The cause type
   */

  private class CausesContains<T : Exception> internal constructor(
    private val exception_type: Class<T>,
    private val message: String) : BaseMatcher<org.nypl.simplified.profiles.api.ProfileDatabaseException>() {

    override fun matches(item: Any): Boolean {
      if (item is org.nypl.simplified.profiles.api.ProfileDatabaseException) {
        for (c in item.causes()) {
          LOG.error("Cause: ", c)
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

  @Test
  @Throws(Exception::class)
  fun testOpenExistingNotDirectory() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")
    FileUtilities.fileWriteUTF8(f_pro, "Hello!")

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileDatabaseException::class.java)
    this.expected.expect(CausesContains(IOException::class.java, "Not a directory"))
    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)
  }

  /**
   * A subdirectory that can't be parsed as a UUID will be migrated.
   */

  @Test
  @Throws(Exception::class)
  fun testOpenExistingBadSubdirectory() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    f_pro.mkdirs()
    val f_bad = File(f_pro, "not-a-number")
    f_bad.mkdirs()

    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)
  }

  private fun accountsDatabases(): org.nypl.simplified.accounts.database.api.AccountsDatabaseFactoryType {
    return org.nypl.simplified.accounts.database.AccountsDatabases
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONMissing() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    f_pro.mkdirs()
    val f_0 = File(f_pro, "0")
    f_0.mkdirs()

    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingJSONUnparseable() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    f_pro.mkdirs()
    val f_0 = File(f_pro, "0")
    f_0.mkdirs()
    val f_p = File(f_0, "profile.json")
    FileUtilities.fileWriteUTF8(f_p, "} { this is not JSON { } { }")

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileDatabaseException::class.java)
    this.expected.expect(CausesContains(IOException::class.java, "Could not parse profile: "))
    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenExistingEmpty() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    Assert.assertEquals(0, db.profiles().size.toLong())
    Assert.assertEquals(f_pro, db.directory())
    Assert.assertTrue(db.currentProfile().isNone)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenCreateProfiles() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val account_providers = this.accountProviders()

    val db = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      account_providers,
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db.createProfile(acc, "Kermit")
    val p1 = db.createProfile(acc, "Gonzo")
    val p2 = db.createProfile(acc, "Beaker")

    Assert.assertFalse("Profile is not anonymous", p0.isAnonymous)
    Assert.assertFalse("Profile is not anonymous", p1.isAnonymous)
    Assert.assertFalse("Profile is not anonymous", p2.isAnonymous)

    Assert.assertEquals("Kermit", p0.displayName())
    Assert.assertEquals("Gonzo", p1.displayName())
    Assert.assertEquals("Beaker", p2.displayName())

    Assert.assertNotEquals(p0.id(), p1.id())
    Assert.assertNotEquals(p0.id(), p2.id())
    Assert.assertNotEquals(p1.id(), p2.id())

    Assert.assertTrue(
      "Kermit profile exists",
      p0.directory().isDirectory)

    Assert.assertTrue(
      "Kermit profile file exists",
      File(p0.directory(), "profile.json").isFile)

    Assert.assertTrue(
      "Gonzo profile exists",
      p1.directory().isDirectory)

    Assert.assertTrue(
      "Gonzo profile file exists",
      File(p1.directory(), "profile.json").isFile)

    Assert.assertTrue(
      "Beaker profile exists",
      p1.directory().isDirectory)

    Assert.assertTrue(
      "Beaker profile file exists",
      File(p2.directory(), "profile.json").isFile)

    Assert.assertFalse(p0.isCurrent)
    Assert.assertFalse(p1.isCurrent)
    Assert.assertFalse(p2.isCurrent)

    Assert.assertEquals(
      account_providers.provider(URI.create("http://www.example.com/accounts0/")),
      p0.accountCurrent().provider())
    Assert.assertEquals(
      account_providers.provider(URI.create("http://www.example.com/accounts0/")),
      p1.accountCurrent().provider())
    Assert.assertEquals(
      account_providers.provider(URI.create("http://www.example.com/accounts0/")),
      p2.accountCurrent().provider())

    Assert.assertTrue(db.currentProfile().isNone)
  }

  @Test
  @Throws(Exception::class)
  fun testOpenCreateReopen() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val account_providers = this.accountProviders()

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      account_providers,
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")
    val p1 = db0.createProfile(acc, "Gonzo")
    val p2 = db0.createProfile(acc, "Beaker")

    val db1 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      account_providers,
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val pr0 = db1.profiles()[p0.id()]
    val pr1 = db1.profiles()[p1.id()]
    val pr2 = db1.profiles()[p2.id()]

    Assert.assertEquals(p0.directory(), pr0!!.directory())
    Assert.assertEquals(p1.directory(), pr1!!.directory())
    Assert.assertEquals(p2.directory(), pr2!!.directory())

    Assert.assertEquals(p0.displayName(), pr0!!.displayName())
    Assert.assertEquals(p1.displayName(), pr1!!.displayName())
    Assert.assertEquals(p2.displayName(), pr2!!.displayName())

    Assert.assertEquals(p0.id(), pr0!!.id())
    Assert.assertEquals(p1.id(), pr1!!.id())
    Assert.assertEquals(p2.id(), pr2!!.id())
  }

  @Test
  @Throws(Exception::class)
  fun testOpenCreateUpdatePreferences() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db.createProfile(acc, "Kermit")
    p0.preferencesUpdate(
      p0.preferences()
        .toBuilder()
        .setDateOfBirth(ProfileDateOfBirth(LocalDate(2010, 10, 30), true))
        .build())

    Assert.assertEquals(
      Option.some(ProfileDateOfBirth(LocalDate(2010, 10, 30), true)),
      p0.preferences().dateOfBirth())
  }

  @Test
  @Throws(Exception::class)
  fun testCreateProfileDuplicate() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db.createProfile(acc, "Kermit")

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileDatabaseException::class.java)
    this.expected.expectMessage(StringContains.containsString("Display name is already used"))
    db.createProfile(acc, "Kermit")
  }

  @Test
  @Throws(Exception::class)
  fun testCreateProfileEmptyDisplayName() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileDatabaseException::class.java)
    this.expected.expectMessage(StringContains.containsString("Display name cannot be empty"))
    db.createProfile(acc, "")
  }

  @Test
  @Throws(Exception::class)
  fun testSetCurrent() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")

    db0.setProfileCurrent(p0.id())

    Assert.assertTrue(p0.isCurrent)
    Assert.assertEquals(Option.some(p0), db0.currentProfile())
  }

  @Test
  @Throws(Exception::class)
  fun testSetCurrentNonexistent() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileNonexistentException::class.java)
    this.expected.expectMessage(StringContains.containsString("Profile does not exist"))
    db0.setProfileCurrent(org.nypl.simplified.profiles.api.ProfileID(UUID.fromString("135dec78-b89b-4a6c-bf6a-294c1694d40b")))
  }

  @Test
  @Throws(Exception::class)
  fun testAnonymous() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileEnabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      exampleAccountProvider(),
      f_pro)

    Assert.assertEquals(1L, db0.profiles().size.toLong())

    val p0 = db0.anonymousProfile()
    Assert.assertTrue("Anonymous profile must be enabled", p0.isAnonymous)
    Assert.assertEquals(Option.some(p0), db0.currentProfile())
  }

  @Test
  @Throws(Exception::class)
  fun testAnonymousSetCurrent() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileEnabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      exampleAccountProvider(),
      f_pro)

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileAnonymousEnabledException::class.java)
    db0.setProfileCurrent(org.nypl.simplified.profiles.api.ProfileID.generate())
  }

  @Test
  @Throws(Exception::class)
  fun testCreateDelete() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc0 = fakeProvider("http://www.example.com/accounts0/")
    val acc1 = fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id())

    val a0 = p0.createAccount(acc1)
    Assert.assertTrue("Account must exist", p0.accounts().containsKey(a0.id()))
    p0.deleteAccountByProvider(acc1)
    Assert.assertFalse("Account must not exist", p0.accounts().containsKey(a0.id()))
  }

  @Test
  @Throws(Exception::class)
  fun testDeleteUnknownProvider() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc0 = fakeProvider("http://www.example.com/accounts0/")
    val acc1 = fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id())

    this.expected.expect(org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException::class.java)
    p0.deleteAccountByProvider(acc1)
  }

  @Test
  @Throws(Exception::class)
  fun testDeleteLastAccount() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc0 = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id())

    this.expected.expect(org.nypl.simplified.accounts.database.api.AccountsDatabaseLastAccountException::class.java)
    p0.deleteAccountByProvider(acc0)
  }

  @Test
  @Throws(Exception::class)
  fun testSetCurrentAccount() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc0 = fakeProvider("http://www.example.com/accounts0/")
    val acc1 = fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id())

    val ac1 = p0.createAccount(acc1)
    Assert.assertNotEquals(ac1, p0.accountCurrent())
    p0.selectAccount(acc1)
    Assert.assertEquals(ac1, p0.accountCurrent())
  }

  @Test
  @Throws(Exception::class)
  fun testSetCurrentAccountUnknown() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    val acc0 = fakeProvider("http://www.example.com/accounts0/")
    val acc1 = fakeProvider("http://www.example.com/accounts1/")

    val p0 = db0.createProfile(acc0, "Kermit")
    db0.setProfileCurrent(p0.id())

    this.expected.expect(org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException::class.java)
    p0.selectAccount(acc1)
  }

  @Test
  @Throws(Exception::class)
  fun testAnonymousNotEnabled() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      this.accountProviders(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      this.accountsDatabases(),
      f_pro)

    this.expected.expect(org.nypl.simplified.profiles.api.ProfileAnonymousDisabledException::class.java)
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
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val account_providers = accountProviders()

    val db0 = org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      account_providers,
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      accountsDatabases(),
      f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")

    val p0 = db0.createProfile(acc, "Kermit")
    p0.createAccount(account_providers.provider(URI.create("http://www.example.com/accounts1/")))

    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      accountProvidersMissingOne(),
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      accountsDatabases(),
      f_pro)
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
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val account_providers_no_zero = accountProvidersMissingZero()

    val db0 =
      org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
        this.context(),
        this.accountEvents,
        account_providers_no_zero,
        org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        accountsDatabases(),
        f_pro)

    val p0 = db0.createProfile(account_providers_no_zero.providerDefault(), "Kermit")

    val account_providers_no_one = accountProvidersMissingOne()

    org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
      this.context(),
      this.accountEvents,
      account_providers_no_one,
      org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
      this.credentialStore,
      accountsDatabases(),
      f_pro)
  }

  /**
   * Repeatedly reopening a database in anonymous/non-anonymous mode doesn't cause any damage.
   *
   * @throws Exception On errors
   */

  @Test
  @Throws(Exception::class)
  fun testOpenAnonymousNonAnonymousAlternating() {
    val f_tmp = DirectoryUtilities.directoryCreateTemporary()
    val f_pro = File(f_tmp, "profiles")

    val account_providers = accountProviders()

    val db0 =
      org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileDisabled(
        this.context(),
        this.accountEvents,
        account_providers,
        org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        accountsDatabases(),
        f_pro)

    val acc = fakeProvider("http://www.example.com/accounts0/")
    val p0 = db0.createProfile(acc, "Kermit")
    val acc0 =
      p0.createAccount(account_providers.provider(URI.create("http://www.example.com/accounts1/")))

    val db1 =
      org.nypl.simplified.profiles.ProfilesDatabase.openWithAnonymousProfileEnabled(
        this.context(),
        this.accountEvents,
        account_providers,
        org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty.getInstance(),
        this.credentialStore,
        accountsDatabases(),
        acc,
        f_pro)

    val p1 = db1.anonymousProfile()

    Assert.assertTrue(p0.accounts().containsKey(acc0.id()))
    Assert.assertTrue(p0.accounts().containsKey(acc0.id()))
  }


  private fun accountProvidersMissingZero(): AccountProviderCollectionType {
    val p1 = fakeProvider("http://www.example.com/accounts1/")
    val providers = TreeMap<URI, AccountProviderType>()
    providers[p1.id] = p1
    return org.nypl.simplified.accounts.database.AccountProviderCollection.create(p1, providers)
  }

  private fun accountProvidersMissingOne(): AccountProviderCollectionType {
    val p0 = fakeProvider("http://www.example.com/accounts0/")
    val providers = TreeMap<URI, AccountProviderType>()
    providers[p0.id] = p0
    return org.nypl.simplified.accounts.database.AccountProviderCollection.create(p0, providers)
  }

  private fun accountProviders(): AccountProviderCollectionType {
    val p0 = fakeProvider("http://www.example.com/accounts0/")
    val p1 = fakeProvider("http://www.example.com/accounts1/")
    val providers = TreeMap<URI, AccountProviderType>()
    providers[p0.id] = p0
    providers[p1.id] = p1
    return org.nypl.simplified.accounts.database.AccountProviderCollection.create(p0, providers)
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(ProfilesDatabaseContract::class.java)

    private fun exampleAccountProvider(): AccountProviderType {
      return AccountProviders.builder()
        .apply {
          this.catalogURI = URI.create("http://www.example.com")
          this.supportEmail = "postmaster@example.com"
          this.id = URI.create("urn:com.example")
          this.mainColor = "#eeeeee"
          this.logo = URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo=")
          this.subtitle = "Example Subtitle"
          this.displayName = "Example Provider"
          this.annotationsURI = URI.create("http://example.com/accounts0/annotations")
          this.patronSettingsURI = URI.create("http://example.com/accounts0/patrons/me")
        }.build()
    }

    private fun fakeProvider(provider_id: String): AccountProviderType {
      return AccountProviders.builder()
        .apply {
          this.id = URI.create(provider_id)
          this.mainColor = "#ff0000"
          this.displayName = "Fake Library"
          this.subtitle = "Imaginary books"
          this.logo = URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo=")
          this.catalogURI = URI.create("http://www.example.com/accounts0/feed.xml")
          this.supportEmail = "postmaster@example.com"
          this.annotationsURI = URI.create("http://example.com/accounts0/annotations")
          this.patronSettingsURI = URI.create("http://example.com/accounts0/patrons/me")
        }.build()
    }
  }
}
