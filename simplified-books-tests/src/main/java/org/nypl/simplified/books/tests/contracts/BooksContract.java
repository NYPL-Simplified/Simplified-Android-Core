package org.nypl.simplified.books.tests.contracts;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
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
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksStatusCacheType;
import org.nypl.simplified.books.core.BooksType;
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
import org.nypl.simplified.test.utilities.TestUtilities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default implementation of the {@link BooksContractType}.
 */

public final class BooksContract implements BooksContractType
{
  private static final URI LOANS_URI = URI.create("http://example.com/loans");
  private static final URI ROOT_URI  = URI.create("http://example.com/");

  /**
   * Construct a contract.
   */

  public BooksContract()
  {

  }

  private static HTTPType makeAuthHTTP(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    final Map<String, List<String>> empty_headers =
      new HashMap<String, List<String>>();

    return new HTTPType()
    {

      @Override public HTTPResultType<InputStream> get(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri,
        final long offset)
      {
        if (uri.equals(BooksContract.LOANS_URI)) {
          return this.getLoans(auth_opt);
        }

        return new HTTPResultOK<InputStream>(
          "OK",
          200,
          new ByteArrayInputStream("DATA".getBytes()),
          4L,
          empty_headers,
          0L);
      }

      @Override public HTTPResultType<InputStream> put(
          final OptionType<HTTPAuthType> auth_opt,
          final URI uri)
      {
        return this.get(auth_opt, uri, 0);
      }

      @Override
      public HTTPResultType<InputStream> post(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final byte[] data,
        final String content_type)
      {
        return new HTTPResultOK<InputStream>(
          "OK",
          200,
          new ByteArrayInputStream("DATA".getBytes()),
          4L,
          empty_headers,
          0L);
      }

      private HTTPResultType<InputStream> getLoans(
        final OptionType<HTTPAuthType> auth_opt)
      {
        if (auth_opt.isNone()) {
          return this.unauthorized();
        }

        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        try {
          return auth.matchAuthType(
            new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>()
            {
              private boolean isAuthorized(
                final HTTPAuthBasic b)
              {
                boolean ok = b.getUser().equals(barcode.toString());
                ok = ok && b.getPassword().equals(pin.toString());
                return ok;
              }

              @Override public HTTPResultType<InputStream> onAuthBasic(
                final HTTPAuthBasic b)
                throws IOException
              {
                final boolean ok = this.isAuthorized(b);
                if (ok == false) {
                  return unauthorized();
                }

                final InputStream stream =
                  BooksContract.class.getResourceAsStream(
                    "/org/nypl/simplified/books/tests/contracts/loans.xml");

                return new HTTPResultOK<InputStream>(
                  "OK", 200, stream, 1L, empty_headers, 0L);
              }

              @Override
              public HTTPResultType<InputStream> onAuthOAuth(HTTPAuthOAuth b)
                throws IOException {
                return null;
              }
            });
        } catch (final IOException e) {
          throw new UnreachableCodeException(e);
        }
      }

      @Override public HTTPResultType<InputStream> head(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri)
      {
        if (uri.equals(BooksContract.LOANS_URI)) {
          return this.headLoans(auth_opt);
        }

        return new HTTPResultOK<InputStream>(
          "OK", 200, new ByteArrayInputStream("DATA".getBytes()), 1L, empty_headers, 0L);
      }

      private HTTPResultType<InputStream> headLoans(
        final OptionType<HTTPAuthType> auth_opt)
      {
        if (auth_opt.isNone()) {
          return this.unauthorized();
        }

        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        try {
          return auth.matchAuthType(
            new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>()
            {
              private boolean isAuthorized(
                final HTTPAuthBasic b)
              {
                boolean ok = b.getUser().equals(barcode.toString());
                ok = ok && b.getPassword().equals(pin.toString());
                return ok;
              }

              @Override public HTTPResultType<InputStream> onAuthBasic(
                final HTTPAuthBasic b)
                throws IOException
              {
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

      private <T> HTTPResultType<T> unauthorized()
      {
        final OptionType<HTTPProblemReport> report = Option.none();
        return new HTTPResultError<T>(
          401,
          "Unauthorized",
          0L,
          empty_headers,
          0L,
          new ByteArrayInputStream(new byte[0]),
          report);
      }
    };
  }

  private static HTTPType makeExceptionHTTP()
  {
    return new HTTPType()
    {
      @Override public HTTPResultType<InputStream> get(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final long offset)
      {
        return new HTTPResultException<InputStream>(uri, new IOException());
      }

      @Override public HTTPResultType<InputStream> put(
        final OptionType<HTTPAuthType> auth,
        final URI uri)
      {
        return new HTTPResultException<InputStream>(uri, new IOException());
      }

      @Override public HTTPResultType<InputStream> head(
        final OptionType<HTTPAuthType> auth,
        final URI uri)
      {
        return new HTTPResultException<InputStream>(uri, new IOException());
      }

      @Override
      public HTTPResultType<InputStream> post(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final byte[] data,
        final String content_type)
      {
        return new HTTPResultException<InputStream>(uri, new IOException());
      }
    };
  }

  private static FeedLoaderType newParser(final BookDatabaseReadableType db)
  {
    final OPDSFeedParserType in_parser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final ExecutorService in_exec = Executors.newSingleThreadExecutor();
    final HTTPType http = HTTP.newHTTP();
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport =
      FeedHTTPTransport.newTransport(http);
    final OPDSSearchParserType in_search_parser = OPDSSearchParser.newParser();
    return FeedLoader.newFeedLoader(
      in_exec, db, in_parser, in_transport, in_search_parser);
  }

  private static DocumentStoreType newFakeDocumentStore()
  {
    return new DocumentStoreType()
    {
      @Override public OptionType<SyncedDocumentType> getPrivacyPolicy()
      {
        return Option.none();
      }

      @Override public OptionType<SyncedDocumentType> getAcknowledgements()
      {
        return Option.none();
      }

      @Override public OptionType<SyncedDocumentType> getAbout()
      {
        return Option.none();
      }

      @Override public AuthenticationDocumentType getAuthenticationDocument()
      {
        return new AuthenticationDocumentType()
        {
          @Override public String getLabelLoginUserID()
          {
            return "Login";
          }

          @Override public String getLabelLoginPassword()
          {
            return "Password";
          }

          @Override
          public String getLabelLoginPatronName() {
            return "Name";
          }

          @Override public void documentUpdate(final InputStream data)
          {
            // Nothing
          }
        };
      }

      @Override public OptionType<EULAType> getEULA()
      {
        return Option.none();
      }

      @Override
      public OptionType<SyncedDocumentType> getLicenses() {
        return Option.none();
      }
    };
  }

  @Override public void testBooksLoadFileNotDirectory()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = File.createTempFile("books", "");

      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();
      final HTTPType in_http = BooksContract.makeExceptionHTTP();

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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(
        new AccountDataLoadListenerType()
        {
          @Override public void onAccountDataBookLoadFailed(
            final BookID id,
            final OptionType<Throwable> error,
            final String message)
          {
            System.out.println("testBooksLoadFileNotDirectory: load failed");
            ok.set(false);
          }

          @Override public void onAccountDataBookLoadFinished()
          {
            System.out.println("testBooksLoadFileNotDirectory: load finished");
          }

          @Override public void onAccountDataBookLoadSucceeded(
            final BookID book,
            final BookDatabaseEntrySnapshot snap)
          {
            System.out.println("testBooksLoadFileNotDirectory: load succeeded");
            ok.set(false);
          }

          @Override public void onAccountDataLoadFailedImmediately(
            final Throwable error)
          {
            System.out.println("testBooksLoadFileNotDirectory: load failed");
            ok.set(false);
          }

          @Override public void onAccountUnavailable()
          {
            System.out.println(
              "testBooksLoadFileNotDirectory: account unavailable");
            ok.set(true);
            latch.countDown();
          }
        });

      latch.await(10L, TimeUnit.SECONDS);
      TestUtilities.assertEquals(Boolean.valueOf(ok.get()), Boolean.TRUE);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoadNotLoggedIn()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();
      final HTTPType in_http = BooksContract.makeExceptionHTTP();
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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(
        new AccountDataLoadListenerType()
        {
          @Override public void onAccountDataBookLoadFailed(
            final BookID id,
            final OptionType<Throwable> error,
            final String message)
          {
            System.out.println("testBooksLoadNotLoggedIn: load failed");
            ok.set(false);
          }

          @Override public void onAccountDataBookLoadFinished()
          {
            System.out.println("testBooksLoadNotLoggedIn: load finished");
          }

          @Override public void onAccountDataBookLoadSucceeded(
            final BookID book,
            final BookDatabaseEntrySnapshot snap)
          {
            System.out.println("testBooksLoadNotLoggedIn: load succeeded");
            ok.set(false);
          }

          @Override public void onAccountDataLoadFailedImmediately(
            final Throwable error)
          {
            System.out.println("testBooksLoadNotLoggedIn: load failed");
            ok.set(false);
          }

          @Override public void onAccountUnavailable()
          {
            System.out.println("testBooksLoadNotLoggedIn: account unavailable");
            ok.set(true);
            latch.countDown();
          }
        });

      latch.await(10L, TimeUnit.SECONDS);
      TestUtilities.assertEquals(Boolean.valueOf(ok.get()), Boolean.TRUE);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginAcceptedFirst()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
        new AccountCredentials(no_vendor, barcode, pin,Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean succeeded = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType listener = new AccountLoginListenerType()
      {
        @Override
        public void onAccountSyncAuthenticationFailure(final String message)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountSyncBook(final BookID book)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountSyncFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountSyncSuccess()
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountSyncBookDeleted(final BookID book)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountLoginFailureCredentialsIncorrect()
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountLoginFailureServerError(final int code)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountLoginFailureLocalError(
          final OptionType<Throwable> error,
          final String message)
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountLoginSuccess(
          final AccountCredentials credentials)
        {
          try {
            System.out.println("testBooksLoginAcceptedFirst: logged in");
            succeeded.set(true);
          } finally {
            latch.countDown();
          }
        }

        @Override public void onAccountLoginFailureDeviceActivationError(
          final String message)
        {
          throw new UnreachableCodeException();
        }
      };

      b.accountLogin(creds, listener);

      latch.await(10L, TimeUnit.SECONDS);
      TestUtilities.assertEquals(
        Boolean.valueOf(succeeded.get()), Boolean.TRUE);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginFileNotDirectory()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = File.createTempFile("books", "");
      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
        new AccountCredentials(no_vendor, barcode, pin,Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final AtomicBoolean failed = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType()
        {
          @Override
          public void onAccountSyncAuthenticationFailure(final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncBook(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncSuccess()
          {
            // Nothing
          }

          @Override public void onAccountSyncBookDeleted(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureCredentialsIncorrect()
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureServerError(final int code)
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureLocalError(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.out.println(
                "testBooksLoginFileNotDirectory: login failed: " + message);
              ((Some<Throwable>) error).get().printStackTrace();
              failed.set(true);
            } finally {
              latch.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountCredentials credentials)
          {
            throw new UnreachableCodeException();
          }

          @Override public void onAccountLoginFailureDeviceActivationError(
            final String message)
          {
            // Nothing
          }
        };

      b.accountLogin(creds, login_listener);

      latch.await(10L, TimeUnit.SECONDS);
      TestUtilities.assertEquals(Boolean.valueOf(failed.get()), Boolean.TRUE);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksSyncLoadLogoutOK()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
        new AccountCredentials(no_vendor, barcode, pin,Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final CountDownLatch latch0 = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType()
        {

          @Override
          public void onAccountSyncAuthenticationFailure(final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncBook(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncSuccess()
          {
            // Nothing
          }

          @Override public void onAccountSyncBookDeleted(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureCredentialsIncorrect()
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureServerError(final int code)
          {
            try {
              System.out.println(
                "testBooksSyncLoadOK: login failed: " + code);
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginFailureLocalError(
            final OptionType<Throwable> error,
            final String message)
          {
            // Nothing
          }

          @Override public void onAccountLoginSuccess(
            final AccountCredentials credentials)
          {
            try {
              System.out.println("testBooksSyncLoadOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginFailureDeviceActivationError(
            final String message)
          {
            // Nothing
          }
        };

      System.out.println("starting login");
      b.accountLogin(creds, login_listener);
      System.out.println("awaiting login completion");
      latch0.await(10L, TimeUnit.SECONDS);
      System.out.println("login completed");

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType()
        {

          @Override public void onAccountSyncAuthenticationFailure(
            final String message)
          {
            try {
              ok.set(false);
              System.out.println(
                "testBooksSyncLoadOK: login failed: " + message);
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncBook(
            final BookID book)
          {
            System.out.println(
              "onAccountSyncBook: synced: " + book);
            count.incrementAndGet();
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              ok.set(false);
              System.out.println(
                "testBooksSyncLoadOK: login failed: " + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncSuccess()
          {
            ok.set(true);
            latch1.countDown();
          }

          @Override public void onAccountSyncBookDeleted(final BookID book)
          {

          }
        };

      System.out.println("syncing account");
      b.accountSync(sync_listener);
      System.out.println("awaiting account sync");
      latch1.await(10L, TimeUnit.SECONDS);
      System.out.println("account synced");

      TestUtilities.assertEquals(Boolean.valueOf(ok.get()), Boolean.TRUE);
      TestUtilities.assertEquals(
        Integer.valueOf(count.get()), Integer.valueOf(4));

      ok.set(false);
      count.set(0);
      final CountDownLatch latch2 = new CountDownLatch(4);

      final AccountDataLoadListenerType load_listener =
        new AccountDataLoadListenerType()
        {

          @Override public void onAccountDataBookLoadFailed(
            final BookID id,
            final OptionType<Throwable> error,
            final String message)
          {
            ok.set(false);
          }

          @Override public void onAccountDataBookLoadFinished()
          {
            // Nothing
          }

          @Override public void onAccountDataBookLoadSucceeded(
            final BookID book,
            final BookDatabaseEntrySnapshot snap)
          {
            try {
              count.incrementAndGet();
              ok.set(true);
            } finally {
              latch2.countDown();
            }
          }

          @Override public void onAccountDataLoadFailedImmediately(
            final Throwable error)
          {
            ok.set(false);
          }

          @Override public void onAccountUnavailable()
          {
            ok.set(false);
          }
        };

      System.out.println("loading books");
      b.accountLoadBooks(load_listener);
      System.out.println("waiting for book load completion");
      latch2.await(10L, TimeUnit.SECONDS);
      System.out.println("book load completed");

      TestUtilities.assertEquals(Boolean.valueOf(ok.get()), Boolean.TRUE);
      TestUtilities.assertEquals(
        Integer.valueOf(count.get()), Integer.valueOf(4));

      final CountDownLatch latch3 = new CountDownLatch(1);
      final AccountLogoutListenerType logout_listener =
        new AccountLogoutListenerType()
        {
          @Override public void onAccountLogoutFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            latch3.countDown();
          }

          @Override public void onAccountLogoutSuccess()
          {
            latch3.countDown();
          }

          @Override
          public void onAccountLogoutFailureServerError(int code) {

          }
        };

      System.out.println("logging out");
      b.accountLogout(creds,logout_listener, sync_listener);
      System.out.println("awaiting logout completion");
      latch3.await(10L, TimeUnit.SECONDS);
      System.out.println("logged out");

      final File data = new File(tmp, "data");
      TestUtilities.assertTrue(data.exists() == false);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksSyncOK()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = DirectoryUtilities.directoryCreateTemporary();
      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final OptionType<AdobeVendorID> no_vendor = Option.none();
      final AccountCredentials creds =
        new AccountCredentials(no_vendor, barcode, pin,Option.some(new AccountAuthProvider("Library")));

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

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
        BooksContract.newFakeDocumentStore(),
        database,
        accounts,
        books_config,
        books_config.getCurrentRootFeedURI().resolve("loans/"));

      final CountDownLatch latch0 = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType()
        {

          @Override
          public void onAccountSyncAuthenticationFailure(final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncBook(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            // Nothing
          }

          @Override public void onAccountSyncSuccess()
          {
            // Nothing
          }

          @Override public void onAccountSyncBookDeleted(final BookID book)
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureCredentialsIncorrect()
          {
            // Nothing
          }

          @Override public void onAccountLoginFailureServerError(final int code)
          {
            try {
              System.out.println("testBooksSyncOK: login failed: " + code);
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginFailureLocalError(
            final OptionType<Throwable> error,
            final String message)
          {
            // Nothing

          }

          @Override public void onAccountLoginSuccess(
            final AccountCredentials credentials)
          {
            try {
              System.out.println("testBooksSyncOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginFailureDeviceActivationError(
            final String message)
          {
            // Nothing
          }
        };

      b.accountLogin(creds, login_listener);

      latch0.await(10L, TimeUnit.SECONDS);

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType()
        {

          @Override public void onAccountSyncAuthenticationFailure(
            final String message)
          {
            try {
              ok.set(false);
              System.out.println("testBooksSyncOK: sync failed: " + message);
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncBook(
            final BookID book)
          {
            System.out.println("testBooksSyncOK: sync: " + book);
            count.incrementAndGet();
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              ok.set(false);
              System.out.println("testBooksSyncOK: sync failed: " + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncSuccess()
          {
            ok.set(true);
            latch1.countDown();
          }

          @Override public void onAccountSyncBookDeleted(final BookID book)
          {
            System.out.println("testBooksSyncOK: delete: " + book);
          }
        };

      b.accountSync(sync_listener);
      latch1.await(10L, TimeUnit.SECONDS);

      TestUtilities.assertEquals(Boolean.valueOf(ok.get()), Boolean.TRUE);
      TestUtilities.assertEquals(
        Integer.valueOf(count.get()), Integer.valueOf(4));

      /**
       * Assert status of each book.
       */

      final BooksStatusCacheType status_cache = b.bookGetStatusCache();

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
          BookID.exactString(
            "561c5ecf0d3020e18ff66e17db27ca232898d409e1d7b0a0432dbea848a1abfe"
            + ""));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
        TestUtilities.assertEquals(o, o);
      }

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
          BookID.exactString(
            "28a0d7122f93e0e052e9e50b35531d01d55056d8fbd3c853e307a0455888150e"
            + ""));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
        TestUtilities.assertEquals(o, o);
      }

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
          BookID.exactString(
            "8e697815fb146a0ffd0bb3776b8197cea1bd6cb75a95a34053bf2b65e0b7e7e7"
            + ""));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
        TestUtilities.assertEquals(o, o);
      }

      {
        final OptionType<BookStatusType> opt = status_cache.booksStatusGet(
          BookID.exactString(
            "284a2dc4e2852f1a69665aa28949e8659cf9d7d53ca11c7bf096403261368ade"
            + ""));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
        TestUtilities.assertEquals(o, o);
      }

    } finally {
      exec.shutdown();
    }
  }

  private final static class BooksControllerConfiguration
    implements BooksControllerConfigurationType
  {
    private URI current_root;

    BooksControllerConfiguration()
    {
      this.current_root = NullCheck.notNull(BooksContract.ROOT_URI);
    }

    @Override public synchronized URI getCurrentRootFeedURI()
    {
      return this.current_root;
    }

    @Override public synchronized void setCurrentRootFeedURI(final URI u)
    {
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
}
