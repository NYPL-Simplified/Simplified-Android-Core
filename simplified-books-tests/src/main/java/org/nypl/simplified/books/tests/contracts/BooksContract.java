package org.nypl.simplified.books.tests.contracts;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfiguration;
import org.nypl.simplified.books.core.BooksControllerConfigurationBuilderType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthMatcherType;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultOK;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.test.utilities.TestUtilities;

import com.google.common.io.Files;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class BooksContract implements
  BooksContractType
{
  private static final URI LOANS_URI = URI.create("http://example.com/loans");

  private static HTTPType makeAuthHTTP(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    final Map<String, List<String>> empty_headers =
      new HashMap<String, List<String>>();

    return new HTTPType() {

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
          4,
          empty_headers);
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
          return auth
            .matchAuthType(new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>() {
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
                  BooksContract.class
                    .getResourceAsStream("/org/nypl/simplified/books/tests/contracts/loans.xml");

                return new HTTPResultOK<InputStream>(
                  "OK",
                  200,
                  stream,
                  1,
                  empty_headers);
              }
            });
        } catch (final IOException e) {
          throw new UnreachableCodeException(e);
        }
      }

      @Override public HTTPResultType<Unit> head(
        final OptionType<HTTPAuthType> auth_opt,
        final URI uri)
      {
        if (uri.equals(BooksContract.LOANS_URI)) {
          return this.headLoans(auth_opt);
        }

        return new HTTPResultOK<Unit>(
          "OK",
          200,
          Unit.unit(),
          1,
          empty_headers);
      }

      private HTTPResultType<Unit> headLoans(
        final OptionType<HTTPAuthType> auth_opt)
      {
        if (auth_opt.isNone()) {
          return this.unauthorized();
        }

        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        try {
          return auth
            .matchAuthType(new HTTPAuthMatcherType<HTTPResultType<Unit>, IOException>() {
              private boolean isAuthorized(
                final HTTPAuthBasic b)
              {
                boolean ok = b.getUser().equals(barcode.toString());
                ok = ok && b.getPassword().equals(pin.toString());
                return ok;
              }

              @Override public HTTPResultType<Unit> onAuthBasic(
                final HTTPAuthBasic b)
                throws IOException
              {
                final boolean ok = this.isAuthorized(b);
                if (ok == false) {
                  return unauthorized();
                }

                return new HTTPResultOK<Unit>(
                  "OK",
                  200,
                  Unit.unit(),
                  1,
                  empty_headers);
              }
            });
        } catch (final IOException e) {
          throw new UnreachableCodeException(e);
        }
      }

      private <T> HTTPResultType<T> unauthorized()
      {
        return new HTTPResultError<T>(401, "Unauthorized", 0, empty_headers);
      }
    };
  }

  public BooksContract()
  {

  }

  private HTTPType makeExceptionHTTP()
  {
    return new HTTPType() {
      @Override public HTTPResultType<InputStream> get(
        final OptionType<HTTPAuthType> auth,
        final URI uri,
        final long offset)
      {
        return new HTTPResultException<InputStream>(new IOException());
      }

      @Override public HTTPResultType<Unit> head(
        final OptionType<HTTPAuthType> auth,
        final URI uri)
      {
        return new HTTPResultException<Unit>(new IOException());
      }
    };
  }

  @Override public void testBooksLoadFileNotDirectory()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      tmp.delete();
      tmp.createNewFile();

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      final BooksControllerConfiguration in_config = bcb.build();
      final HTTPType in_http = this.makeExceptionHTTP();

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final AtomicBoolean ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(new AccountDataLoadListenerType() {
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
          final BookSnapshot snap)
        {
          System.out.println("testBooksLoadFileNotDirectory: load succeeded");
          ok.set(false);
        }

        @Override public void onAccountUnavailable()
        {
          System.out
            .println("testBooksLoadFileNotDirectory: account unavailable");
          ok.set(true);
          latch.countDown();
        }
      });

      latch.await();
      TestUtilities.assertEquals(ok.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoadNotLoggedIn()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      final BooksControllerConfiguration in_config = bcb.build();
      final HTTPType in_http = this.makeExceptionHTTP();

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final AtomicBoolean ok = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);
      b.accountLoadBooks(new AccountDataLoadListenerType() {
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
          System.out.println("testBooksLoadNotLoggedIn: load succeeded");
          ok.set(false);
        }

        @Override public void onAccountDataBookLoadSucceeded(
          final BookID book,
          final BookSnapshot snap)
        {
          System.out.println("testBooksLoadNotLoggedIn: load succeeded");
          ok.set(false);
        }

        @Override public void onAccountUnavailable()
        {
          System.out.println("testBooksLoadNotLoggedIn: account unavailable");
          ok.set(true);
          latch.countDown();
        }
      });

      latch.await();
      TestUtilities.assertEquals(ok.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginAcceptedFirst()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksControllerConfiguration in_config = bcb.build();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());
      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final AtomicBoolean rejected = new AtomicBoolean(false);
      final AtomicBoolean succeeded = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {

          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used_barcode,
            final AccountPIN used_pin)
          {
            try {
              System.out.println("testBooksLoginAcceptedFirst: logged in");
              succeeded.set(true);
            } finally {
              latch.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin, listener);

      latch.await();
      TestUtilities.assertEquals(rejected.get(), false);
      TestUtilities.assertEquals(succeeded.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginFileNotDirectory()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      tmp.delete();
      tmp.createNewFile();

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksControllerConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final AtomicBoolean failed = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.err
                .println("testBooksLoginFileNotDirectory: login failed: "
                  + message);
              ((Some<Throwable>) error).get().printStackTrace();
              failed.set(true);
            } finally {
              latch.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used_barcode,
            final AccountPIN used_pin)
          {
            throw new UnreachableCodeException();
          }
        };

      b.accountLogin(barcode, pin, login_listener);

      latch.await();
      TestUtilities.assertEquals(failed.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksSyncFileNotDirectory()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksControllerConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.err
                .println("testBooksSyncFileNotDirectory: login failed: "
                  + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used_barcode,
            final AccountPIN used_pin)
          {
            try {
              System.err
                .println("testBooksSyncFileNotDirectory: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin, login_listener);

      latch0.await();

      final File data = new File(tmp, "data");
      new File(data, "credentials.txt").delete();
      data.delete();
      tmp.delete();
      tmp.createNewFile();

      TestUtilities.assertTrue(tmp.isFile());

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean failed = new AtomicBoolean(false);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {
          @Override public void onAccountSyncAuthenticationFailure(
            final String message)
          {
            try {
              failed.set(true);
              System.err
                .println("testBooksSyncFileNotDirectory: login failed: "
                  + message);
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncBook(
            final BookID book)
          {
            System.err.println("testBooksSyncFileNotDirectory: synced book: "
              + book);
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              failed.set(true);
              System.err
                .println("testBooksSyncFileNotDirectory: login failed: "
                  + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncSuccess()
          {
            System.err.println("testBooksSyncFileNotDirectory: synced");
          }
        };

      b.accountSync(sync_listener);
      latch1.await();
      TestUtilities.assertEquals(failed.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksSyncLoadLogoutOK()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksControllerConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.err.println("testBooksSyncLoadOK: login failed: "
                + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used_barcode,
            final AccountPIN used_pin)
          {
            try {
              System.err.println("testBooksSyncLoadOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin, login_listener);

      latch0.await();

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {

          @Override public void onAccountSyncAuthenticationFailure(
            final String message)
          {
            try {
              ok.set(false);
              System.err.println("testBooksSyncLoadOK: login failed: "
                + message);
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncBook(
            final BookID book)
          {
            count.incrementAndGet();
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              ok.set(false);
              System.err.println("testBooksSyncLoadOK: login failed: "
                + message);
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
        };

      b.accountSync(sync_listener);
      latch1.await();

      TestUtilities.assertEquals(ok.get(), true);
      TestUtilities.assertEquals(count.get(), 4);

      ok.set(false);
      count.set(0);
      final CountDownLatch latch2 = new CountDownLatch(4);

      final AccountDataLoadListenerType load_listener =
        new AccountDataLoadListenerType() {

          @Override public void onAccountDataBookLoadFailed(
            final BookID id,
            final OptionType<Throwable> error,
            final String message)
          {
            ok.set(false);
          }

          @Override public void onAccountDataBookLoadFinished()
          {

          }

          @Override public void onAccountDataBookLoadSucceeded(
            final BookID book,
            final BookSnapshot snap)
          {
            try {
              count.incrementAndGet();
              ok.set(true);
            } finally {
              latch2.countDown();
            }
          }

          @Override public void onAccountUnavailable()
          {
            ok.set(false);
          }
        };

      b.accountLoadBooks(load_listener);

      latch2.await();

      TestUtilities.assertEquals(ok.get(), true);
      TestUtilities.assertEquals(count.get(), 4);

      final CountDownLatch latch3 = new CountDownLatch(1);
      final AccountLogoutListenerType logout_listener =
        new AccountLogoutListenerType() {
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
        };

      b.accountLogout(logout_listener);

      latch3.await();

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
      final File tmp = Files.createTempDir();

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksControllerConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);

      final DownloaderType d =
        Downloader.newDownloader(exec, in_http, DownloaderConfiguration
          .newBuilder(Files.createTempDir())
          .build());

      final BooksType b =
        BooksController.newBooks(
          exec,
          OPDSFeedParser.newParser(),
          in_http,
          d,
          in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);

      final AccountLoginListenerType login_listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.err.println("testBooksSyncOK: login failed: " + message);
              ((Some<Throwable>) error).get().printStackTrace();
            } finally {
              latch0.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used_barcode,
            final AccountPIN used_pin)
          {
            try {
              System.err.println("testBooksSyncOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin, login_listener);

      latch0.await();

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {

          @Override public void onAccountSyncAuthenticationFailure(
            final String message)
          {
            try {
              ok.set(false);
              System.err.println("testBooksSyncOK: sync failed: " + message);
            } finally {
              latch1.countDown();
            }
          }

          @Override public void onAccountSyncBook(
            final BookID book)
          {
            System.err.println("testBooksSyncOK: sync: " + book);
            count.incrementAndGet();
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              ok.set(false);
              System.err.println("testBooksSyncOK: sync failed: " + message);
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
        };

      b.accountSync(sync_listener);
      latch1.await();

      TestUtilities.assertEquals(ok.get(), true);
      TestUtilities.assertEquals(count.get(), 4);

      /**
       * Assert status of each book.
       */

      {
        final OptionType<BookStatusType> opt =
          b
            .booksStatusGet(BookID
              .exactString("561c5ecf0d3020e18ff66e17db27ca232898d409e1d7b0a0432dbea848a1abfe"));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
      }

      {
        final OptionType<BookStatusType> opt =
          b
            .booksStatusGet(BookID
              .exactString("28a0d7122f93e0e052e9e50b35531d01d55056d8fbd3c853e307a0455888150e"));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
      }

      {
        final OptionType<BookStatusType> opt =
          b
            .booksStatusGet(BookID
              .exactString("8e697815fb146a0ffd0bb3776b8197cea1bd6cb75a95a34053bf2b65e0b7e7e7"));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
      }

      {
        final OptionType<BookStatusType> opt =
          b
            .booksStatusGet(BookID
              .exactString("284a2dc4e2852f1a69665aa28949e8659cf9d7d53ca11c7bf096403261368ade"));
        final Some<BookStatusType> some = (Some<BookStatusType>) opt;
        final BookStatusLoaned o = (BookStatusLoaned) some.get();
      }

    } finally {
      exec.shutdown();
    }
  }
}
