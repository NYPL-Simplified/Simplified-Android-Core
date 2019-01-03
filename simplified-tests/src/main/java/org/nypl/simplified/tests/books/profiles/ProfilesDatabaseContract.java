package org.nypl.simplified.tests.books.profiles;

import com.io7m.jfunctional.Option;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.StringContains;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountProviderCollectionType;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseFactoryType;
import org.nypl.simplified.books.accounts.AccountsDatabaseLastAccountException;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.accounts.AccountsDatabases;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.profiles.ProfileAnonymousDisabledException;
import org.nypl.simplified.books.profiles.ProfileAnonymousEnabledException;
import org.nypl.simplified.books.profiles.ProfileDatabaseException;
import org.nypl.simplified.books.profiles.ProfileID;
import org.nypl.simplified.books.profiles.ProfileNonexistentException;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabase;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class ProfilesDatabaseContract {

  private static final Logger LOG = LogUtilities.getLog(ProfilesDatabaseContract.class);

  @Rule
  public ExpectedException expected = ExpectedException.none();

  /**
   * An exception matcher that checks to see if the given profile database exception has
   * at least one cause of the given type and with the given exception message.
   *
   * @param <T> The cause type
   */

  private static final class CausesContains<T extends Exception>
      extends BaseMatcher<ProfileDatabaseException> {
    private final Class<T> exception_type;
    private final String message;

    CausesContains(
        final Class<T> exception_type,
        final String message) {
      this.exception_type = exception_type;
      this.message = message;
    }

    @Override
    public boolean matches(final Object item) {
      if (item instanceof ProfileDatabaseException) {
        final ProfileDatabaseException ex = (ProfileDatabaseException) item;
        for (final Exception c : ex.causes()) {
          LOG.error("Cause: ", c);
          if (exception_type.isAssignableFrom(c.getClass()) && c.getMessage().contains(message)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("must throw ProfileDatabaseException");
      description.appendText(" with at least one cause of type " + exception_type);
      description.appendText(" with a message containing '" + message + "'");
    }
  }

  @Test
  public final void testOpenExistingNotDirectory()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");
    FileUtilities.fileWriteUTF8(f_pro, "Hello!");

    expected.expect(ProfileDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Not a directory"));
    ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
  }

  @Test
  public final void testOpenExistingBadSubdirectory()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    f_pro.mkdirs();
    final File f_bad = new File(f_pro, "not-a-number");
    f_bad.mkdirs();

    expected.expect(ProfileDatabaseException.class);
    expected.expect(new CausesContains<>(
        IOException.class, "Could not parse directory name as profile ID"));
    ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
  }

  private AccountsDatabaseFactoryType accountsDatabases()
  {
    return AccountsDatabases.get();
  }

  @Test
  public final void testOpenExistingJSONMissing()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    f_pro.mkdirs();
    final File f_0 = new File(f_pro, "0");
    f_0.mkdirs();

    expected.expect(ProfileDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Could not parse profile: "));
    ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
  }

  @Test
  public final void testOpenExistingJSONUnparseable()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    f_pro.mkdirs();
    final File f_0 = new File(f_pro, "0");
    f_0.mkdirs();
    final File f_p = new File(f_0, "profile.json");
    FileUtilities.fileWriteUTF8(f_p, "} { this is not JSON { } { }");

    expected.expect(ProfileDatabaseException.class);
    expected.expect(new CausesContains<>(IOException.class, "Could not parse profile: "));
    ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
  }

  @Test
  public final void testOpenExistingEmpty()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    Assert.assertEquals(0, db.profiles().size());
    Assert.assertEquals(f_pro, db.directory());
    Assert.assertTrue(db.currentProfile().isNone());
  }

  @Test
  public final void testOpenCreateProfiles()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final AccountProviderCollectionType account_providers = accountProviders();

    final ProfilesDatabaseType db =
        ProfilesDatabase.openWithAnonymousAccountDisabled(
            account_providers, accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db.createProfile(acc, "Kermit");
    final ProfileType p1 = db.createProfile(acc, "Gonzo");
    final ProfileType p2 = db.createProfile(acc, "Beaker");

    Assert.assertFalse("Profile is not anonymous", p0.isAnonymous());
    Assert.assertFalse("Profile is not anonymous", p1.isAnonymous());
    Assert.assertFalse("Profile is not anonymous", p2.isAnonymous());

    Assert.assertEquals("Kermit", p0.displayName());
    Assert.assertEquals("Gonzo", p1.displayName());
    Assert.assertEquals("Beaker", p2.displayName());

    Assert.assertNotEquals(p0.id(), p1.id());
    Assert.assertNotEquals(p0.id(), p2.id());
    Assert.assertNotEquals(p1.id(), p2.id());

    Assert.assertTrue(
        "Kermit profile exists",
        p0.directory().isDirectory());

    Assert.assertTrue(
        "Kermit profile file exists",
        new File(p0.directory(), "profile.json").isFile());

    Assert.assertTrue(
        "Gonzo profile exists",
        p1.directory().isDirectory());

    Assert.assertTrue(
        "Gonzo profile file exists",
        new File(p1.directory(), "profile.json").isFile());

    Assert.assertTrue(
        "Beaker profile exists",
        p1.directory().isDirectory());

    Assert.assertTrue(
        "Beaker profile file exists",
        new File(p2.directory(), "profile.json").isFile());

    Assert.assertFalse(p0.isCurrent());
    Assert.assertFalse(p1.isCurrent());
    Assert.assertFalse(p2.isCurrent());

    Assert.assertEquals(
        account_providers.provider(URI.create("http://www.example.com/accounts0/")),
        p0.accountCurrent().provider());
    Assert.assertEquals(
        account_providers.provider(URI.create("http://www.example.com/accounts0/")),
        p1.accountCurrent().provider());
    Assert.assertEquals(
        account_providers.provider(URI.create("http://www.example.com/accounts0/")),
        p2.accountCurrent().provider());

    Assert.assertTrue(db.currentProfile().isNone());
  }

  @Test
  public final void testOpenCreateReopen()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final AccountProviderCollectionType account_providers = accountProviders();

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(
            account_providers, accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db0.createProfile(acc, "Kermit");
    final ProfileType p1 = db0.createProfile(acc, "Gonzo");
    final ProfileType p2 = db0.createProfile(acc, "Beaker");

    final ProfilesDatabaseType db1 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(
            account_providers, accountsDatabases(), f_pro);

    final ProfileType pr0 = db1.profiles().get(p0.id());
    final ProfileType pr1 = db1.profiles().get(p1.id());
    final ProfileType pr2 = db1.profiles().get(p2.id());

    Assert.assertEquals(p0.directory(), pr0.directory());
    Assert.assertEquals(p1.directory(), pr1.directory());
    Assert.assertEquals(p2.directory(), pr2.directory());

    Assert.assertEquals(p0.displayName(), pr0.displayName());
    Assert.assertEquals(p1.displayName(), pr1.displayName());
    Assert.assertEquals(p2.displayName(), pr2.displayName());

    Assert.assertEquals(p0.id(), pr0.id());
    Assert.assertEquals(p1.id(), pr1.id());
    Assert.assertEquals(p2.id(), pr2.id());
  }

  @Test
  public final void testOpenCreateUpdatePreferences()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db.createProfile(acc, "Kermit");
    p0.preferencesUpdate(
        p0.preferences()
            .toBuilder()
            .setDateOfBirth(new LocalDate(2010, 10, 30))
            .build());

    Assert.assertEquals(
        Option.some(new LocalDate(2010, 10, 30)),
        p0.preferences().dateOfBirth());
  }

  @Test
  public final void testCreateProfileDuplicate()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db.createProfile(acc, "Kermit");

    expected.expect(ProfileDatabaseException.class);
    expected.expectMessage(StringContains.containsString("Display name is already used"));
    db.createProfile(acc, "Kermit");
  }

  @Test
  public final void testCreateProfileEmptyDisplayName()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    expected.expect(ProfileDatabaseException.class);
    expected.expectMessage(StringContains.containsString("Display name cannot be empty"));
    db.createProfile(acc, "");
  }

  @Test
  public final void testSetCurrent()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db0.createProfile(acc, "Kermit");

    db0.setProfileCurrent(p0.id());

    Assert.assertTrue(p0.isCurrent());
    Assert.assertEquals(Option.some(p0), db0.currentProfile());
  }

  @Test
  public final void testSetCurrentNonexistent()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    final AccountProvider acc = fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db0.createProfile(acc, "Kermit");

    expected.expect(ProfileNonexistentException.class);
    expected.expectMessage(StringContains.containsString("Profile does not exist"));
    db0.setProfileCurrent(ProfileID.create(23));
  }

  @Test
  public final void testAnonymous()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountEnabled(
            accountProviders(), accountsDatabases(), exampleAccountProvider(), f_pro);

    Assert.assertEquals(1L, db0.profiles().size());

    final ProfileType p0 = db0.anonymousProfile();
    Assert.assertTrue("Anonymous profile must be enabled", p0.isAnonymous());
    Assert.assertEquals(Option.some(p0), db0.currentProfile());
  }

  @Test
  public final void testAnonymousSetCurrent()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountEnabled(
            accountProviders(), accountsDatabases(), exampleAccountProvider(), f_pro);

    expected.expect(ProfileAnonymousEnabledException.class);
    db0.setProfileCurrent(ProfileID.create(23));
  }

  @Test
  public final void testCreateDelete()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
    final AccountProvider acc0 =
        fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider acc1 =
        fakeProvider("http://www.example.com/accounts1/");

    final ProfileType p0 = db0.createProfile(acc0, "Kermit");
    db0.setProfileCurrent(p0.id());

    final AccountType a0 = p0.createAccount(acc1);
    Assert.assertTrue("Account must exist", p0.accounts().containsKey(a0.id()));
    p0.deleteAccountByProvider(acc1);
    Assert.assertFalse("Account must not exist", p0.accounts().containsKey(a0.id()));
  }

  @Test
  public final void testDeleteUnknownProvider()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
    final AccountProvider acc0 =
        fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider acc1 =
        fakeProvider("http://www.example.com/accounts1/");

    final ProfileType p0 = db0.createProfile(acc0, "Kermit");
    db0.setProfileCurrent(p0.id());

    expected.expect(AccountsDatabaseNonexistentException.class);
    p0.deleteAccountByProvider(acc1);
  }

  @Test
  public final void testDeleteLastAccount()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
    final AccountProvider acc0 =
        fakeProvider("http://www.example.com/accounts0/");

    final ProfileType p0 = db0.createProfile(acc0, "Kermit");
    db0.setProfileCurrent(p0.id());

    expected.expect(AccountsDatabaseLastAccountException.class);
    p0.deleteAccountByProvider(acc0);
  }

  @Test
  public final void testSetCurrentAccount()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
    final AccountProvider acc0 =
        fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider acc1 =
        fakeProvider("http://www.example.com/accounts1/");

    final ProfileType p0 = db0.createProfile(acc0, "Kermit");
    db0.setProfileCurrent(p0.id());

    final AccountType ac1 = p0.createAccount(acc1);
    Assert.assertNotEquals(ac1, p0.accountCurrent());
    p0.selectAccount(acc1);
    Assert.assertEquals(ac1, p0.accountCurrent());
  }

  @Test
  public final void testSetCurrentAccountUnknown() throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);
    final AccountProvider acc0 =
        fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider acc1 =
        fakeProvider("http://www.example.com/accounts1/");

    final ProfileType p0 = db0.createProfile(acc0, "Kermit");
    db0.setProfileCurrent(p0.id());

    expected.expect(AccountsDatabaseNonexistentException.class);
    p0.selectAccount(acc1);
  }

  private static AccountProvider exampleAccountProvider() {
    return AccountProvider.builder()
        .setCatalogURI(URI.create("http://www.example.com"))
        .setSupportEmail("postmaster@example.com")
        .setId(URI.create("urn:com.example"))
        .setMainColor("#eeeeee")
        .setLogo(URI.create("http://www.example.com/logo.png"))
        .setSubtitle("Example Subtitle")
        .setDisplayName("Example Provider")
        .build();
  }

  @Test
  public final void testAnonymousNotEnabled()
      throws Exception {
    final File f_tmp = DirectoryUtilities.directoryCreateTemporary();
    final File f_pro = new File(f_tmp, "profiles");

    final ProfilesDatabaseType db0 =
        ProfilesDatabase.openWithAnonymousAccountDisabled(accountProviders(), accountsDatabases(), f_pro);

    expected.expect(ProfileAnonymousDisabledException.class);
    expected.expectMessage(StringContains.containsString("The anonymous profile is not enabled"));
    db0.anonymousProfile();
  }

  private AccountProviderCollectionType accountProviders() {
    final AccountProvider p0 = fakeProvider("http://www.example.com/accounts0/");
    final AccountProvider p1 = fakeProvider("http://www.example.com/accounts1/");
    final SortedMap<URI, AccountProvider> providers = new TreeMap<>();
    providers.put(p0.id(), p0);
    providers.put(p1.id(), p1);
    return AccountProviderCollection.create(p0, providers);
  }

  private static AccountProvider fakeProvider(final String provider_id) {
    return AccountProvider.builder()
        .setId(URI.create(provider_id))
        .setDisplayName("Fake Library")
        .setSubtitle("Imaginary books")
        .setLogo(URI.create("http://www.example.com/logo.png"))
        .setCatalogURI(URI.create("http://www.example.com/accounts0/feed.xml"))
        .setSupportEmail("postmaster@example.com")
        .build();
  }
}
