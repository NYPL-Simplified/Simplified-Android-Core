package org.nypl.simplified.tests.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountBarcode;
import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountPIN;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabases;
import org.nypl.simplified.books.analytics.AnalyticsLogger;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookEvent;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistry;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookStatusEvent;
import org.nypl.simplified.books.book_registry.BookStatusLoaned;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.bundled_content.BundledContentResolverType;
import org.nypl.simplified.books.controller.BooksControllerType;
import org.nypl.simplified.books.controller.Controller;
import org.nypl.simplified.books.core.BookFormats;
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoCredentials;
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoURI;
import org.nypl.simplified.books.feeds.FeedHTTPTransport;
import org.nypl.simplified.books.feeds.FeedLoader;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.profiles.ProfileDatabaseException;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabase;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultOK;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.nypl.simplified.opds.core.OPDSSearchParser;
import org.nypl.simplified.tests.EventAssertions;
import org.nypl.simplified.tests.http.MockingHTTP;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BooksControllerContract {

  @Rule public ExpectedException expected = ExpectedException.none();

  private ExecutorService executor_downloads;
  private ExecutorService executor_books;
  private File directory_downloads;
  private File directory_profiles;
  private MockingHTTP http;
  private List<ProfileEvent> profile_events;
  private List<AccountEvent> account_events;
  private DownloaderType downloader;
  private BookRegistryType book_registry;
  private ProfilesDatabaseType profiles;
  private List<BookEvent> book_events;
  private ExecutorService executor_timer;

  private static AccountProvider fakeProvider(final String provider_id) {
    return AccountProvider.builder()
        .setId(URI.create(provider_id))
        .setDisplayName("Fake Library")
        .setSubtitle("Imaginary books")
        .setLogo(URI.create("http://example.com/logo.png"))
        .setCatalogURI(URI.create("http://example.com/accounts0/feed.xml"))
        .setSupportEmail("postmaster@example.com")
        .build();
  }

  private static AccountProviderCollection accountProviders(final Unit unit) {
    return accountProviders();
  }

  private static AccountProviderCollection accountProviders() {
    final AccountProvider fake0 = fakeProvider("urn:fake:0");
    final AccountProvider fake1 = fakeProvider("urn:fake:1");
    final AccountProvider fake2 = fakeProvider("urn:fake:2");
    final AccountProvider fake3 = fakeAuthProvider("urn:fake-auth:0");

    final SortedMap<URI, AccountProvider> providers = new TreeMap<>();
    providers.put(fake0.id(), fake0);
    providers.put(fake1.id(), fake1);
    providers.put(fake2.id(), fake2);
    providers.put(fake3.id(), fake3);
    return AccountProviderCollection.create(fake0, providers);
  }

  private static AccountProvider fakeAuthProvider(final String uri) {
    return fakeProvider(uri)
        .toBuilder()
        .setAuthentication(Option.some(AccountProviderAuthenticationDescription.builder()
            .setLoginURI(URI.create(uri))
            .setPassCodeLength(4)
            .setPassCodeMayContainLetters(true)
            .build()))
        .build();
  }

  private static OptionType<AccountAuthenticationCredentials> correctCredentials() {
    return Option.of(
        AccountAuthenticationCredentials.builder(
            AccountPIN.create("1234"), AccountBarcode.create("abcd"))
            .build());
  }

  private BooksControllerType controller(
      final ExecutorService exec,
      final HTTPType http,
      final BookRegistryType books,
      final ProfilesDatabaseType profiles,
      final DownloaderType downloader,
      final FunctionType<Unit, AccountProviderCollection> account_providers,
      final ExecutorService timer_exec) {

    final OPDSFeedParserType parser =
        OPDSFeedParser.newParser(
          OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes()));
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> transport =
        FeedHTTPTransport.newTransport(http);
    final BundledContentResolverType bundled_content = uri -> {
      throw new FileNotFoundException(uri.toString());
    };

    final FeedLoaderType feed_loader =
        FeedLoader.newFeedLoader(exec, books, bundled_content, parser, transport, OPDSSearchParser.newParser());

    final File analytics_directory =
        new File("/tmp/aulfa-android-tests");

    final AnalyticsLogger analytics_logger =
        AnalyticsLogger.create(analytics_directory);

    return Controller.create(
        exec,
        http,
        parser,
        feed_loader,
        downloader,
        profiles,
        analytics_logger,
        books,
        bundled_content,
        account_providers,
        timer_exec);
  }

  @Before
  public void setUp() throws Exception {
    this.http = new MockingHTTP();
    this.executor_downloads = Executors.newCachedThreadPool();
    this.executor_books = Executors.newCachedThreadPool();
    this.executor_timer = Executors.newCachedThreadPool();
    this.directory_downloads = DirectoryUtilities.directoryCreateTemporary();
    this.directory_profiles = DirectoryUtilities.directoryCreateTemporary();
    this.profile_events = Collections.synchronizedList(new ArrayList<ProfileEvent>());
    this.profiles = profilesDatabaseWithoutAnonymous(this.directory_profiles);
    this.account_events = Collections.synchronizedList(new ArrayList<AccountEvent>());
    this.book_events = Collections.synchronizedList(new ArrayList<BookEvent>());
    this.book_registry = BookRegistry.create();
    this.downloader = DownloaderHTTP.newDownloader(this.executor_downloads, this.directory_downloads, this.http);
  }

  @After
  public void tearDown() throws Exception {
    this.executor_books.shutdown();
    this.executor_downloads.shutdown();
    this.executor_timer.shutdown();
  }

  /**
   * If the remote side returns a non 401 error code, syncing should fail with an IO exception.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncRemoteNon401() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultError<>(
            400,
            "BAD REQUEST",
            0L,
            new HashMap<>(),
            0L,
            new ByteArrayInputStream(new byte[0]),
            Option.none()));

    this.expected.expect(ExecutionException.class);
    this.expected.expectCause(IsInstanceOf.instanceOf(IOException.class));
    controller.booksSync(account).get();
  }

  /**
   * If the remote side returns a 401 error code, the current credentials should be thrown away.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncRemote401() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    Assert.assertTrue(account.credentials().isSome());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultError<>(
            401,
            "UNAUTHORIZED",
            0L,
            new HashMap<>(),
            0L,
            new ByteArrayInputStream(new byte[0]),
            Option.none()));

    controller.booksSync(account).get();

    Assert.assertTrue(account.credentials().isNone());
  }

  /**
   * If the provider does not support authentication, then syncing is impossible and does nothing.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncWithoutAuthSupport() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeProvider("urn:fake:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    Assert.assertTrue(account.credentials().isSome());
    controller.booksSync(account).get();
    Assert.assertTrue(account.credentials().isSome());
  }

  /**
   * If the remote side requires authentication but no credentials were provided, nothing happens.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncMissingCredentials() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);

    Assert.assertTrue(account.credentials().isNone());
    controller.booksSync(account).get();
    Assert.assertTrue(account.credentials().isNone());
  }

  /**
   * If the remote side returns garbage for a feed, an error is raised.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncBadFeed() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            new ByteArrayInputStream(new byte[]{0x23, 0x10, 0x39, 0x59}),
            4L,
            new HashMap<>(),
            0L));

    this.expected.expect(ExecutionException.class);
    this.expected.expectCause(IsInstanceOf.instanceOf(OPDSParseException.class));
    controller.booksSync(account).get();
  }

  /**
   * If the remote side returns books the account doesn't have, new database entries are created.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncNewEntries() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksSyncNewEntries.xml"),
            resourceSize("testBooksSyncNewEntries.xml"),
            new HashMap<>(),
            0L));

    this.book_registry.bookEvents().subscribe(this.book_events::add);

    Assert.assertEquals(0L, this.book_registry.books().size());
    controller.booksSync(account).get();
    Assert.assertEquals(3L, this.book_registry.books().size());

    this.book_registry.bookOrException(
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"));
    this.book_registry.bookOrException(
        BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f"));
    this.book_registry.bookOrException(
        BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113"));

    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        0,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED));
    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        1,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED));
    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        2,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED));
  }

  /**
   * If the remote side returns few books than the account has, database entries are removed.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksSyncRemoveEntries() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    /*
     * Populate the database by syncing against a feed that contains books.
     */

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksSyncNewEntries.xml"),
            resourceSize("testBooksSyncNewEntries.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    this.book_registry.bookOrException(
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"));
    this.book_registry.bookOrException(
        BookID.create("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f"));
    this.book_registry.bookOrException(
        BookID.create("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113"));

    this.book_registry.bookEvents().subscribe(this.book_events::add);

    /*
     * Now run the sync again but this time with a feed that removes books.
     */

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksSyncRemoveEntries.xml"),
            resourceSize("testBooksSyncRemoveEntries.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();
    Assert.assertEquals(1L, this.book_registry.books().size());

    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        0,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED));
    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        1,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_REMOVED));
    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        2,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_REMOVED));

    this.book_registry.bookOrException(
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f"));

    checkBookIsNotInRegistry("f9a7536a61caa60f870b3fbe9d4304b2d59ea03c71cbaee82609e3779d1e6e0f");
    checkBookIsNotInRegistry("251cc5f69cd2a329bb6074b47a26062e59f5bb01d09d14626f41073f63690113");
  }

  private void checkBookIsNotInRegistry(final String id) {
    try {
      this.book_registry.bookOrException(BookID.create(id));
      Assert.fail("Book should not exist!");
    } catch (final NoSuchElementException e) {
      // Correctly raised
    }
  }

  /**
   * Revoking a book causes a request to be made to the revocation URI.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeCorrectURI() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI.xml"),
            resourceSize("testBooksRevokeCorrectURI.xml"),
            new HashMap<>(),
            0L));

    this.http.addResponse(
        "urn:book:0:revoke",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI_Response.xml"),
            resourceSize("testBooksRevokeCorrectURI_Response.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    this.book_registry.bookEvents().subscribe(this.book_events::add);
    controller.bookRevoke(account, BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f")).get();

    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        0,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_CHANGED));
    EventAssertions.isTypeAndMatches(
        BookStatusEvent.class,
        this.book_events,
        1,
        e -> Assert.assertEquals(e.type(), BookStatusEvent.Type.BOOK_REMOVED));

    checkBookIsNotInRegistry("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");
  }

  /**
   * Revoking a book without credentials fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeWithoutCredentials() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI.xml"),
            resourceSize("testBooksRevokeCorrectURI.xml"),
            new HashMap<>(),
            0L));

    this.http.addResponse(
        "urn:book:0:revoke",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI_Response.xml"),
            resourceSize("testBooksRevokeCorrectURI_Response.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();
    account.setCredentials(Option.none());

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    try {
      controller.bookRevoke(account, book_id).get();
      Assert.fail("Exception must be raised");
    } catch (final ExecutionException e) {
      Assert.assertThat(e.getCause(), IsInstanceOf.instanceOf(BookRevokeExceptionNoCredentials.class));
    }

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusRevokeFailed.class));
  }

  /**
   * Revoking a book that has no revocation URI fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeWithoutURI() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeWithoutURI.xml"),
            resourceSize("testBooksRevokeWithoutURI.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    try {
      controller.bookRevoke(account, book_id).get();
      Assert.fail("Exception must be raised");
    } catch (final ExecutionException e) {
      Assert.assertThat(e.getCause(), IsInstanceOf.instanceOf(BookRevokeExceptionNoURI.class));
    }

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusRevokeFailed.class));
  }

  /**
   * If the server returns an empty feed in response to a revocation, revocation fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeEmptyFeed() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI.xml"),
            resourceSize("testBooksRevokeCorrectURI.xml"),
            new HashMap<>(),
            0L));

    this.http.addResponse(
        "urn:book:0:revoke",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeEmptyFeed.xml"),
            resourceSize("testBooksRevokeEmptyFeed.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    try {
      controller.bookRevoke(account, book_id).get();
      Assert.fail("Exception must be raised");
    } catch (final ExecutionException e) {
      Assert.assertThat(e.getCause(), IsInstanceOf.instanceOf(IOException.class));
    }

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusRevokeFailed.class));
  }

  /**
   * If the server returns a garbage in response to a revocation, revocation fails.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeGarbage() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI.xml"),
            resourceSize("testBooksRevokeCorrectURI.xml"),
            new HashMap<>(),
            0L));

    this.http.addResponse(
        "urn:book:0:revoke",
        new HTTPResultOK<>(
            "OK",
            200,
            new ByteArrayInputStream(new byte[0]),
            0L,
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    try {
      controller.bookRevoke(account, book_id).get();
      Assert.fail("Exception must be raised");
    } catch (final ExecutionException e) {
      Assert.assertThat(e.getCause(), IsInstanceOf.instanceOf(IOException.class));
    }

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusRevokeFailed.class));
  }

  /**
   * Deleting a book works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksDelete() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksDelete.xml"),
            resourceSize("testBooksDelete.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    Assert.assertTrue(
        "Book must not have a saved EPUB file",
        this.book_registry.bookOrException(book_id)
            .book()
            .file()
            .isNone());

    /*
     * Manually reach into the database and create a book in order to have something to delete.
     */

    {
      final BookDatabaseEntryType database_entry = account.bookDatabase().entry(book_id);
      database_entry.writeEPUB(File.createTempFile("book", ".epub"));
      this.book_registry.update(
          BookWithStatus.create(
              database_entry.book(), BookStatus.fromBook(database_entry.book())));
    }

    final OptionType<File> created_file =
        this.book_registry.bookOrException(book_id).book().file();
    Assert.assertTrue(
        "Book must have a saved EPUB file",
        created_file.isSome());

    final File file = ((Some<File>) created_file).get();
    Assert.assertTrue("EPUB must exist", file.isFile());

    this.book_registry.bookEvents().subscribe(this.book_events::add);
    controller.bookDelete(account, book_id).get();

    Assert.assertTrue(
        "Book must not have a saved EPUB file",
        this.book_registry.book(book_id).isNone());

    Assert.assertFalse("EPUB must not exist", file.exists());
  }

  /**
   * Dismissing a failed revocation works.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeDismissOK() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksRevokeCorrectURI.xml"),
            resourceSize("testBooksRevokeCorrectURI.xml"),
            new HashMap<>(),
            0L));

    this.http.addResponse(
        "urn:book:0:revoke",
        new HTTPResultOK<>(
            "OK",
            200,
            new ByteArrayInputStream(new byte[0]),
            0L,
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    try {
      controller.bookRevoke(account, book_id).get();
      Assert.fail("Exception must be raised");
    } catch (final ExecutionException e) {
      Assert.assertThat(e.getCause(), IsInstanceOf.instanceOf(IOException.class));
    }

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusRevokeFailed.class));

    controller.bookRevokeFailedDismiss(account, book_id).get();

    Assert.assertThat(
        this.book_registry.bookOrException(book_id).status(),
        IsInstanceOf.instanceOf(BookStatusLoaned.class));
  }

  /**
   * Dismissing a failed revocation that didn't actually fail does nothing.
   *
   * @throws Exception On errors
   */

  @Test(timeout = 3_000L)
  public final void testBooksRevokeDismissHasNotFailed() throws Exception {

    final BooksControllerType controller =
        controller(this.executor_books, http, this.book_registry, this.profiles, this.downloader, BooksControllerContract::accountProviders, this.executor_timer);

    final AccountProvider provider = fakeAuthProvider("urn:fake-auth:0");
    final ProfileType profile = this.profiles.createProfile(provider, "Kermit");
    this.profiles.setProfileCurrent(profile.id());
    final AccountType account = profile.createAccount(provider);
    account.setCredentials(correctCredentials());

    this.http.addResponse(
        "urn:fake-auth:0",
        new HTTPResultOK<>(
            "OK",
            200,
            resource("testBooksSyncNewEntries.xml"),
            resourceSize("testBooksSyncNewEntries.xml"),
            new HashMap<>(),
            0L));

    controller.booksSync(account).get();

    final BookID book_id =
        BookID.create("39434e1c3ea5620fdcc2303c878da54cc421175eb09ce1a6709b54589eb8711f");

    final BookStatusType status_before = this.book_registry.bookOrException(book_id).status();
    Assert.assertThat(status_before, IsInstanceOf.instanceOf(BookStatusLoaned.class));

    controller.bookRevokeFailedDismiss(account, book_id).get();

    final BookStatusType status_after = this.book_registry.bookOrException(book_id).status();
    Assert.assertEquals(status_before, status_after);
  }

  private InputStream resource(final String file) {
    return BooksControllerContract.class.getResourceAsStream(file);
  }

  private long resourceSize(final String file) throws IOException {
    long total = 0L;
    final byte[] buffer = new byte[8192];
    try (InputStream stream = resource(file)) {
      while (true) {
        final int r = stream.read(buffer);
        if (r <= 0) {
          break;
        }
        total += r;
      }
    }
    return total;
  }

  private ProfilesDatabaseType profilesDatabaseWithoutAnonymous(final File dir_profiles)
      throws ProfileDatabaseException {
    return ProfilesDatabase.openWithAnonymousAccountDisabled(
        accountProviders(Unit.unit()),
        AccountsDatabases.get(),
        dir_profiles);
  }
}
