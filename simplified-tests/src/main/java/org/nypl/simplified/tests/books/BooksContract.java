package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.books.core.AccountAuthProvider;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsDatabase;
import org.nypl.simplified.books.core.AccountsDatabaseType;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookDatabaseReadableType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookFormats;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksStatusCacheType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DeviceActivationListenerType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.FeedHTTPTransport;
import org.nypl.simplified.books.core.FeedLoader;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.SyncedDocumentType;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthMatcherType;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultOK;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.nypl.simplified.opds.core.OPDSSearchParser;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BooksContract {
  private static final Logger LOG = LoggerFactory.getLogger(BooksContract.class);
  private static final URI LOANS_URI = URI.create("http://example.com/loans/");
  private static final URI ROOT_URI = URI.create("http://example.com/");

  private static FeedLoaderType newParser(final BookDatabaseReadableType db) {
    final OPDSFeedParserType in_parser =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.supportedBookMimeTypes()));
    final ExecutorService in_exec = Executors.newSingleThreadExecutor();
    final HTTPType http = HTTP.newHTTP();
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport =
        FeedHTTPTransport.newTransport(http);
    final OPDSSearchParserType in_search_parser = OPDSSearchParser.newParser();
    return FeedLoader.newFeedLoader(
        in_exec, db, in_parser, in_transport, in_search_parser);
  }

  @Test
  public void testBooksLoadFileNotDirectory()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = File.createTempFile("books", "");

      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();
      final HTTPType in_http = new CrashingHTTP();

      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean load_ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(
          new AccountDataLoadListenerType() {
            @Override
            public void onAccountDataBookLoadFailed(
                final BookID id,
                final OptionType<Throwable> error,
                final String message) {
              System.out.println("testBooksLoadFileNotDirectory: load failed");
              load_ok.set(false);
            }

            @Override
            public void onAccountDataBookLoadFinished() {
              System.out.println("testBooksLoadFileNotDirectory: load finished");
            }

            @Override
            public void onAccountDataBookLoadSucceeded(
                final BookID book,
                final BookDatabaseEntrySnapshot snap) {
              System.out.println("testBooksLoadFileNotDirectory: load succeeded");
              load_ok.set(false);
            }

            @Override
            public void onAccountDataLoadFailedImmediately(
                final Throwable error) {
              System.out.println("testBooksLoadFileNotDirectory: load failed");
              load_ok.set(false);
            }

            @Override
            public void onAccountUnavailable() {
              System.out.println(
                  "testBooksLoadFileNotDirectory: account unavailable");
              load_ok.set(true);
              latch.countDown();
            }
          }, true);

      latch.await(10L, TimeUnit.SECONDS);
      Assert.assertEquals("Load must have succeeded", true, load_ok.get());

    } finally {
      exec.shutdown();
    }
  }

  @Test
  public void testBooksLoadNotLoggedIn()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();
      final HTTPType in_http = new CrashingHTTP();
      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean load_ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(
          new AccountDataLoadListenerType() {
            @Override
            public void onAccountDataBookLoadFailed(
                final BookID id,
                final OptionType<Throwable> error,
                final String message) {
              System.out.println("testBooksLoadNotLoggedIn: load failed");
              load_ok.set(false);
            }

            @Override
            public void onAccountDataBookLoadFinished() {
              System.out.println("testBooksLoadNotLoggedIn: load finished");
            }

            @Override
            public void onAccountDataBookLoadSucceeded(
                final BookID book,
                final BookDatabaseEntrySnapshot snap) {
              System.out.println("testBooksLoadNotLoggedIn: load succeeded");
              load_ok.set(false);
            }

            @Override
            public void onAccountDataLoadFailedImmediately(
                final Throwable error) {
              System.out.println("testBooksLoadNotLoggedIn: load failed");
              load_ok.set(false);
            }

            @Override
            public void onAccountUnavailable() {
              System.out.println("testBooksLoadNotLoggedIn: account unavailable");
              load_ok.set(true);
              latch.countDown();
            }
          }, true);

      latch.await(10L, TimeUnit.SECONDS);
      Assert.assertEquals("Load must have succeeded", true, load_ok.get());

    } finally {
      exec.shutdown();
    }
  }

  @Test
  public void testBooksLoginAcceptedFirst()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
          new AccountCredentials(no_vendor, barcode, pin, Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = new AuthenticatedHTTP(barcode, pin);
      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean succeeded = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType listener = new AccountLoginListenerType() {
        @Override
        public void onAccountSyncAuthenticationFailure(final String message) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountSyncBook(final BookID book) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountSyncSuccess() {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountSyncBookDeleted(final BookID book) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountLoginFailureCredentialsIncorrect() {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountLoginFailureServerError(final int code) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountLoginFailureLocalError(
            final OptionType<Throwable> error,
            final String message) {
          throw new UnreachableCodeException();
        }

        @Override
        public void onAccountLoginSuccess(
            final AccountCredentials credentials) {
          try {
            System.out.println("testBooksLoginAcceptedFirst: logged in");
            succeeded.set(true);
          } finally {
            latch.countDown();
          }
        }

        @Override
        public void onAccountLoginFailureDeviceActivationError(
            final String message) {
          throw new UnreachableCodeException();
        }
      };

      b.accountLogin(creds, listener);

      latch.await(10L, TimeUnit.SECONDS);
      Assert.assertEquals("Login must succeed", true, succeeded.get());

    } finally {
      exec.shutdown();
    }
  }

  @Test
  public void testBooksLoginFileNotDirectory()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = File.createTempFile("books", "");
      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
          new AccountCredentials(no_vendor, barcode, pin, Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = new AuthenticatedHTTP(barcode, pin);

      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean failed = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
          new AccountLoginListenerType() {
            @Override
            public void onAccountSyncAuthenticationFailure(final String message) {
              // Nothing
            }

            @Override
            public void onAccountSyncBook(final BookID book) {
              // Nothing
            }

            @Override
            public void onAccountSyncFailure(
                final OptionType<Throwable> error,
                final String message) {
              // Nothing
            }

            @Override
            public void onAccountSyncSuccess() {
              // Nothing
            }

            @Override
            public void onAccountSyncBookDeleted(final BookID book) {
              // Nothing
            }

            @Override
            public void onAccountLoginFailureCredentialsIncorrect() {
              // Nothing
            }

            @Override
            public void onAccountLoginFailureServerError(final int code) {
              // Nothing
            }

            @Override
            public void onAccountLoginFailureLocalError(
                final OptionType<Throwable> error,
                final String message) {
              try {
                System.out.println(
                    "testBooksLoginFileNotDirectory: login failed: " + message);
                ((Some<Throwable>) error).get().printStackTrace();
                failed.set(true);
              } finally {
                latch.countDown();
              }
            }

            @Override
            public void onAccountLoginSuccess(
                final AccountCredentials credentials) {
              throw new UnreachableCodeException();
            }

            @Override
            public void onAccountLoginFailureDeviceActivationError(
                final String message) {
              // Nothing
            }
          };

      b.accountLogin(creds, login_listener);

      latch.await(10L, TimeUnit.SECONDS);
      Assert.assertEquals("Login must fail", true, failed.get());

    } finally {
      exec.shutdown();
    }
  }

  @Test
  public void testBooksSyncLoadLogoutOK()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
          new AccountCredentials(no_vendor, barcode, pin, Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = new AuthenticatedHTTP(barcode, pin);

      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final CountDownLatch login_latch = new CountDownLatch(1);
      final CountDownLatch login_sync_latch = new CountDownLatch(1);
      final AtomicBoolean synced_ok = new AtomicBoolean(false);
      final AtomicInteger synced_book_count = new AtomicInteger(0);

      final AccountLoginListenerType login_listener =
          new LatchedAccountLoginListener(login_sync_latch, synced_book_count, synced_ok, login_latch);

      b.accountLogin(creds, login_listener);
      login_latch.await(10L, TimeUnit.SECONDS);
      login_sync_latch.await(10L, TimeUnit.SECONDS);

      Assert.assertTrue("Sync must succeed", synced_ok.get());
      Assert.assertEquals("Must have synced correct number of books", 2, synced_book_count.get());

      final AtomicInteger load_book_count = new AtomicInteger();
      final CountDownLatch load_latch = new CountDownLatch(2);
      final AtomicBoolean load_ok = new AtomicBoolean();

      final AccountDataLoadListenerType load_listener =
          new AccountDataLoadListenerType() {
            @Override
            public void onAccountDataBookLoadFailed(
                final BookID id,
                final OptionType<Throwable> error,
                final String message) {
              load_ok.set(false);
            }

            @Override
            public void onAccountDataBookLoadFinished() {
              // Nothing
            }

            @Override
            public void onAccountDataBookLoadSucceeded(
                final BookID book,
                final BookDatabaseEntrySnapshot snap) {
              try {
                load_book_count.incrementAndGet();
                load_ok.set(true);
              } finally {
                load_latch.countDown();
              }
            }

            @Override
            public void onAccountDataLoadFailedImmediately(
                final Throwable error) {
              load_ok.set(false);
            }

            @Override
            public void onAccountUnavailable() {
              load_ok.set(false);
            }
          };

      System.out.println("loading books");
      b.accountLoadBooks(load_listener, true);
      System.out.println("waiting for book load completion");
      load_latch.await(10L, TimeUnit.SECONDS);
      System.out.println("book load completed");

      Assert.assertEquals("Loading must succeed", true, load_ok.get());
      Assert.assertEquals("Must have loaded the correct number of books", 2, load_book_count.get());

      final CountDownLatch logout_latch = new CountDownLatch(1);

      final AccountLogoutListenerType logout_listener =
          new AccountLogoutListenerType() {
            @Override
            public void onAccountLogoutFailure(
                final OptionType<Throwable> error,
                final String message) {
              logout_latch.countDown();
            }

            @Override
            public void onAccountLogoutSuccess() {
              logout_latch.countDown();
            }

            @Override
            public void onAccountLogoutFailureServerError(int code) {

            }
          };

      System.out.println("logging out");
      b.accountLogout(creds, logout_listener, new LoggingSyncListener(), new LoggingDeviceListener());
      System.out.println("awaiting logout completion");
      logout_latch.await(10L, TimeUnit.SECONDS);
      System.out.println("logged out");
    } finally {
      exec.shutdown();
    }
  }

  @Test
  public void testBooksSyncOK()
      throws Exception {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
          new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
          new AccountCredentials(no_vendor, barcode, pin, Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = new AuthenticatedHTTP(barcode, pin);

      final OPDSJSONSerializerType in_json_serializer =
          OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final DownloaderType d = DownloaderHTTP.newDownloader(
          exec, DirectoryUtilities.directoryCreateTemporary(), in_http);

      final AccountsDatabaseType accounts =
          AccountsDatabase.openDatabase(new File(tmp, "accounts"));
      final BookDatabaseType database = BookDatabase.newDatabase(
          in_json_serializer, in_json_parser, new File(tmp, "data"));

      final OptionType<AdobeAdeptExecutorType> none = Option.none();
      final BooksType b = BooksController.newBooks(
          exec,
          BooksContract.newParser(database),
          in_http,
          d,
          in_json_serializer,
          in_json_parser,
          none,
          new EmptyDocumentStore(),
          database,
          accounts,
          books_config,
          books_config.getCurrentRootFeedURI().resolve("loans/"));

      final CountDownLatch login_latch = new CountDownLatch(1);
      final CountDownLatch login_sync_latch = new CountDownLatch(1);
      final AtomicBoolean synced_ok = new AtomicBoolean(false);
      final AtomicInteger synced_book_count = new AtomicInteger(0);

      LatchedAccountLoginListener login_listener =
          new LatchedAccountLoginListener(
              login_sync_latch, synced_book_count, synced_ok, login_latch);

      b.accountLogin(creds, login_listener);
      login_latch.await(10L, TimeUnit.SECONDS);
      login_sync_latch.await(10L, TimeUnit.SECONDS);

      Assert.assertTrue("Sync must succeed", synced_ok.get());
      Assert.assertEquals("Must have synced the correct number of books", 2, synced_book_count.get());

      /*
       * Assert status of each book.
       */

      final BooksStatusCacheType status_cache = b.bookGetStatusCache();

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
            BookID.exactString("2925d691731c018650422a6d8463cd1fb880e2a8d0d8741a9652e6fb5a56783f"));
        Assert.assertTrue("Book must have status", opt.isSome());
        final BookStatusType o = ((Some<BookStatusType>) opt).get();
        Assert.assertTrue(
            "Book 2925d691731c018650422a6d8463cd1fb880e2a8d0d8741a9652e6fb5a56783f is loaned",
            o instanceof BookStatusLoaned);
      }

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
            BookID.exactString("31d2a6c5a6aa3065e25a7373167d734d72e72cdd843d1474d807dce2bf6de834"));
        Assert.assertTrue("Book must have status", opt.isSome());
        final BookStatusType o = ((Some<BookStatusType>) opt).get();
        Assert.assertTrue(
            "Book 31d2a6c5a6aa3065e25a7373167d734d72e72cdd843d1474d807dce2bf6de834 is loaned",
            o instanceof BookStatusLoaned);
      }

    } finally {
      exec.shutdown();
    }
  }

  private final static class BooksControllerConfiguration
      implements BooksControllerConfigurationType {
    private URI current_root;

    BooksControllerConfiguration() {
      this.current_root = NullCheck.notNull(BooksContract.ROOT_URI);
    }

    @Override
    public synchronized URI getCurrentRootFeedURI() {
      return this.current_root;
    }

    @Override
    public synchronized void setCurrentRootFeedURI(final URI u) {
      this.current_root = NullCheck.notNull(u);
    }

    @Override
    public URI getAdobeAuthURI() {
      return null;
    }

    @Override
    public void setAdobeAuthURI(URI u) {

    }

    @Override
    public URI getAlternateRootFeedURI() {
      return null;
    }

    @Override
    public void setAlternateRootFeedURI(URI u) {

    }

  }

  /**
   * A device activation listener that simply logs everything.
   */

  private static class LoggingDeviceListener implements DeviceActivationListenerType {
    @Override
    public void onDeviceActivationFailure(String message) {
      LOG.error("onDeviceActivationFailure: {}", message);
    }

    @Override
    public void onDeviceActivationSuccess() {
      LOG.debug("onDeviceActivationSuccess");
    }
  }

  /**
   * An account sync listener that simply logs everything.
   */

  private static class LoggingSyncListener implements AccountSyncListenerType {

    @Override
    public void onAccountSyncAuthenticationFailure(String message) {
      LOG.error("onAccountSyncAuthenticationFailure: {}", message);
    }

    @Override
    public void onAccountSyncBook(BookID book) {
      LOG.debug("onAccountSyncBook: {}", book);
    }

    @Override
    public void onAccountSyncFailure(OptionType<Throwable> error, String message) {
      LOG.error("onAccountSyncFailure: {} {}", error, message);
    }

    @Override
    public void onAccountSyncSuccess() {
      LOG.debug("onAccountSyncSuccess");
    }

    @Override
    public void onAccountSyncBookDeleted(BookID book) {
      LOG.debug("onAccountSyncBookDeleted: {}", book);
    }
  }

  /**
   * An account login listener that can have its progress tracked by waiting on
   * the given latches.
   */

  private static class LatchedAccountLoginListener implements AccountLoginListenerType {
    private final CountDownLatch login_sync_latch;
    private final AtomicInteger synced_book_count;
    private final AtomicBoolean synced_ok;
    private final CountDownLatch login_latch;

    LatchedAccountLoginListener(
        CountDownLatch login_sync_latch,
        AtomicInteger synced_book_count,
        AtomicBoolean synced_ok,
        CountDownLatch login_latch) {
      this.login_sync_latch = login_sync_latch;
      this.synced_book_count = synced_book_count;
      this.synced_ok = synced_ok;
      this.login_latch = login_latch;
    }

    @Override
    public void onAccountSyncAuthenticationFailure(
        final String message) {
      LOG.debug("onAccountSyncAuthenticationFailure: {}", message);
      login_sync_latch.countDown();
    }

    @Override
    public void onAccountSyncBook(
        final BookID book) {
      LOG.debug("onAccountSyncBook: {}", book);
      synced_book_count.incrementAndGet();
    }

    @Override
    public void onAccountSyncFailure(
        final OptionType<Throwable> error,
        final String message) {
      LOG.debug("onAccountSyncFailure: {} {}", error, message);
      login_sync_latch.countDown();
    }

    @Override
    public void onAccountSyncSuccess() {
      LOG.debug("onAccountSyncSuccess");
      synced_ok.set(true);
      login_sync_latch.countDown();
    }

    @Override
    public void onAccountSyncBookDeleted(
        final BookID book) {
      LOG.debug("onAccountSyncBookDeleted: {}", book);
    }

    @Override
    public void onAccountLoginFailureCredentialsIncorrect() {
      try {
        LOG.debug("onAccountLoginFailureCredentialsIncorrect");
      } finally {
        login_latch.countDown();
      }
    }

    @Override
    public void onAccountLoginFailureServerError(final int code) {
      try {
        LOG.debug("onAccountLoginFailureServerError: {}", code);
      } finally {
        login_latch.countDown();
      }
    }

    @Override
    public void onAccountLoginFailureLocalError(
        final OptionType<Throwable> error,
        final String message) {
      LOG.debug("onAccountLoginFailureLocalError: {} {}", error, message);
    }

    @Override
    public void onAccountLoginSuccess(
        final AccountCredentials credentials) {
      try {
        LOG.debug("onAccountLoginSuccess: {}", credentials);
      } finally {
        login_latch.countDown();
      }
    }

    @Override
    public void onAccountLoginFailureDeviceActivationError(
        final String message) {
      LOG.debug("onAccountLoginFailureDeviceActivationError: {}", message);
    }
  }

  /**
   * An HTTP interface that serves a list of loans when presented with the correct credentials.
   */

  private static class AuthenticatedHTTP implements HTTPType {

    private final Map<String, List<String>> empty_headers;
    private final AccountBarcode barcode;
    private final AccountPIN pin;

    public AuthenticatedHTTP(
        final AccountBarcode barcode,
        final AccountPIN pin) {
      this.empty_headers = new HashMap<>();
      this.barcode = barcode;
      this.pin = pin;
    }

    @Override
    public HTTPResultType<InputStream> get(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri,
        final long offset) {
      LOG.debug("get: {} {} {}", auth_opt, uri, offset);

      if (uri.equals(BooksContract.LOANS_URI)) {
        LOG.debug("serving loans");
        return this.getLoans(auth_opt);
      }

      LOG.debug("serving garbage bytes");
      return new HTTPResultOK<InputStream>(
          "OK",
          200,
          new ByteArrayInputStream("DATA".getBytes()),
          4L,
          empty_headers,
          0L);
    }

    @Override
    public HTTPResultType<InputStream> put(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri) {
      LOG.debug("put: {} {}", auth_opt, uri);
      return this.get(auth_opt, uri, 0);
    }

    @Override
    public HTTPResultType<InputStream> post(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final byte[] data,
        final String content_type) {
      LOG.debug("post: {} {} {} {}", auth, uri, data, content_type);
      LOG.debug("serving garbage bytes");
      return new HTTPResultOK<InputStream>(
          "OK",
          200,
          new ByteArrayInputStream("DATA".getBytes()),
          4L,
          empty_headers,
          0L);
    }

    @Override
    public HTTPResultType<InputStream> delete(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final String content_type) {
      LOG.debug("post: {} {} {}", auth, uri, content_type);
      LOG.debug("serving garbage bytes");
      return new HTTPResultOK<InputStream>(
          "OK",
          200,
          new ByteArrayInputStream("DATA".getBytes()),
          4L,
          empty_headers,
          0L);
    }

    private HTTPResultType<InputStream> getLoans(
        final OptionType<HTTPAuthType> auth_opt) {
      if (auth_opt.isNone()) {
        return this.unauthorized();
      }

      final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
      final HTTPAuthType auth = some.get();
      try {
        return auth.matchAuthType(
            new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>() {
              private boolean isAuthorized(
                  final HTTPAuthBasic b) {
                boolean ok = b.getUser().equals(barcode.toString());
                ok = ok && b.getPassword().equals(pin.toString());
                LOG.debug("isAuthorized: {}", ok);
                return ok;
              }

              @Override
              public HTTPResultType<InputStream> onAuthBasic(
                  final HTTPAuthBasic b)
                  throws IOException {
                LOG.debug("onAuthBasic: {}", b);

                final boolean ok = this.isAuthorized(b);
                if (!ok) {
                  return unauthorized();
                }

                final URL resource_url =
                    BooksContract.class.getResource(
                        "/org/nypl/simplified/tests/opds/loans.xml");

                LOG.debug("onAuthBasic: serving {}", resource_url);

                final InputStream stream = resource_url.openStream();
                return new HTTPResultOK<>(
                    "OK",
                    200,
                    stream,
                    1L,
                    empty_headers,
                    0L);
              }

              @Override
              public HTTPResultType<InputStream> onAuthOAuth(HTTPAuthOAuth b)
                  throws IOException {
                LOG.debug("onAuthOAuth: {}", b);
                return null;
              }
            });
      } catch (final IOException e) {
        throw new UnreachableCodeException(e);
      }
    }

    @Override
    public HTTPResultType<InputStream> head(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri) {
      if (uri.equals(BooksContract.LOANS_URI)) {
        return this.headLoans(auth_opt);
      }

      return new HTTPResultOK<InputStream>(
          "OK", 200, new ByteArrayInputStream("DATA".getBytes()), 1L, empty_headers, 0L);
    }

    private HTTPResultType<InputStream> headLoans(
        final OptionType<HTTPAuthType> auth_opt) {
      if (auth_opt.isNone()) {
        return this.unauthorized();
      }

      final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
      final HTTPAuthType auth = some.get();
      try {
        return auth.matchAuthType(
            new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>() {
              private boolean isAuthorized(
                  final HTTPAuthBasic b) {
                boolean ok = b.getUser().equals(barcode.toString());
                ok = ok && b.getPassword().equals(pin.toString());
                return ok;
              }

              @Override
              public HTTPResultType<InputStream> onAuthBasic(
                  final HTTPAuthBasic b)
                  throws IOException {
                final boolean ok = this.isAuthorized(b);
                if (ok == false) {
                  return unauthorized();
                }

                return new HTTPResultOK<InputStream>(
                    "OK", 200, new ByteArrayInputStream("DATA".getBytes()), 1L, empty_headers, 0L);
              }

              @Override
              public HTTPResultType<InputStream> onAuthOAuth(HTTPAuthOAuth b) throws IOException {
                return null;
              }
            });
      } catch (final IOException e) {
        throw new UnreachableCodeException(e);
      }
    }

    private <T> HTTPResultType<T> unauthorized() {
      final OptionType<HTTPProblemReport> report = Option.none();
      return new HTTPResultError<>(
          401,
          "Unauthorized",
          0L,
          empty_headers,
          0L,
          new ByteArrayInputStream(new byte[0]),
          report);
    }
  }

  /**
   * An HTTP interface that raises an exception on every operation.
   */

  private static class CrashingHTTP implements HTTPType {

    @Override
    public HTTPResultType<InputStream> get(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final long offset) {
      return new HTTPResultException<>(uri, new IOException());
    }

    @Override
    public HTTPResultType<InputStream> put(
        final OptionType<HTTPAuthType> auth,
        final URI uri) {
      return new HTTPResultException<>(uri, new IOException());
    }

    @Override
    public HTTPResultType<InputStream> head(
        final OptionType<HTTPAuthType> auth,
        final URI uri) {
      return new HTTPResultException<>(uri, new IOException());
    }

    @Override
    public HTTPResultType<InputStream> post(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final byte[] data,
        final String content_type) {
      return new HTTPResultException<>(uri, new IOException());
    }

    @Override
    public HTTPResultType<InputStream> delete(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final String content_type) {
      return new HTTPResultException<>(uri, new IOException());
    }
  }

  /**
   * A document store implementation that contains no documents.
   */

  private static class EmptyDocumentStore implements DocumentStoreType {

    @Override
    public OptionType<SyncedDocumentType> getPrivacyPolicy() {
      return Option.none();
    }

    @Override
    public OptionType<SyncedDocumentType> getAcknowledgements() {
      return Option.none();
    }

    @Override
    public OptionType<SyncedDocumentType> getAbout() {
      return Option.none();
    }

    @Override
    public AuthenticationDocumentType getAuthenticationDocument() {
      return new AuthenticationDocumentType() {
        @Override
        public String getLabelLoginUserID() {
          return "Login";
        }

        @Override
        public String getLabelLoginPassword() {
          return "Password";
        }

        @Override
        public String getLabelLoginPatronName() {
          return "Name";
        }

        @Override
        public void documentUpdate(final InputStream data) {
          // Nothing
        }
      };
    }

    @Override
    public OptionType<EULAType> getEULA() {
      return Option.none();
    }

    @Override
    public OptionType<SyncedDocumentType> getLicenses() {
      return Option.none();
    }
  }
}
