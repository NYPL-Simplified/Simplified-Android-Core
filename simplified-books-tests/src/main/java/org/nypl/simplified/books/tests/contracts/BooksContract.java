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
import org.nypl.simplified.books.core.AccountPINListenerType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.Book;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.Books;
import org.nypl.simplified.books.core.BooksConfiguration;
import org.nypl.simplified.books.core.BooksConfigurationBuilderType;
import org.nypl.simplified.books.core.BooksType;
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
import com.io7m.jfunctional.Option;
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

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      final BooksConfiguration in_config = bcb.build();
      final HTTPType in_http = this.makeExceptionHTTP();
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

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

        @Override public void onAccountDataBookLoadSucceeded(
          final Book book)
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
      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      final BooksConfiguration in_config = bcb.build();
      final HTTPType in_http = this.makeExceptionHTTP();
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

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

        @Override public void onAccountDataBookLoadSucceeded(
          final Book book)
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
      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final AtomicBoolean requested = new AtomicBoolean(false);
      final AtomicBoolean rejected = new AtomicBoolean(false);
      final AtomicBoolean succeeded = new AtomicBoolean(false);

      final CountDownLatch latch = new CountDownLatch(2);

      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            rejected.set(true);
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            try {
              System.out
                .println("testBooksLoginAcceptedFirst: pin requested");
              requested.set(true);
              return Option.some(pin);
            } finally {
              latch.countDown();
            }
          }
        };

      final AccountLoginListenerType listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {

          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used)
          {
            try {
              System.out.println("testBooksLoginAcceptedFirst: logged in");
              succeeded.set(true);
            } finally {
              latch.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, listener);

      latch.await();
      TestUtilities.assertEquals(requested.get(), true);
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

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final AtomicBoolean failed = new AtomicBoolean(false);
      final CountDownLatch latch = new CountDownLatch(1);

      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            return Option.some(pin);
          }
        };

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
            final AccountBarcode actual)
          {
            throw new UnreachableCodeException();
          }
        };

      b.accountLogin(barcode, pin_listener, login_listener);

      latch.await();
      TestUtilities.assertEquals(failed.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginNoPINGiven()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final AtomicBoolean requested = new AtomicBoolean(false);
      final AtomicBoolean rejected = new AtomicBoolean(false);
      final AtomicBoolean succeeded = new AtomicBoolean(false);

      final CountDownLatch latch = new CountDownLatch(2);

      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            rejected.set(true);
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            try {
              System.out.println("testBooksLoginNoPINGiven: pin requested");
              requested.set(true);
              return Option.none();
            } finally {
              latch.countDown();
            }
          }
        };

      final AccountLoginListenerType listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.out
                .println("testBooksLoginNoPINGiven: failed to log in");
            } finally {
              latch.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used)
          {
            try {
              System.out.println("testBooksLoginNoPINGiven: logged in");
              succeeded.set(true);
            } finally {
              latch.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, listener);

      latch.await();
      TestUtilities.assertEquals(requested.get(), true);
      TestUtilities.assertEquals(rejected.get(), false);
      TestUtilities.assertEquals(succeeded.get(), false);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginRejectedFirstAcceptedSecond()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final AtomicBoolean requested = new AtomicBoolean(false);
      final AtomicBoolean rejected = new AtomicBoolean(false);
      final AtomicBoolean succeeded = new AtomicBoolean(false);

      final CountDownLatch latch = new CountDownLatch(3);

      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstAcceptedSecond: pin rejected");
              rejected.set(true);
              return Option.some(pin);
            } finally {
              latch.countDown();
            }
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstAcceptedSecond: pin requested");
              requested.set(true);
              return Option.some(new AccountPIN("wrong"));
            } finally {
              latch.countDown();
            }
          }
        };

      final AccountLoginListenerType listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {

          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used)
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstAcceptedSecond: logged in");
              succeeded.set(true);
            } finally {
              latch.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, listener);

      latch.await();
      TestUtilities.assertEquals(requested.get(), true);
      TestUtilities.assertEquals(rejected.get(), true);
      TestUtilities.assertEquals(succeeded.get(), true);

    } finally {
      exec.shutdown();
    }
  }

  @Override public void testBooksLoginRejectedFirstGaveUpSecond()
    throws Exception
  {
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    try {
      final File tmp = Files.createTempDir();
      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();

      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");

      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final AtomicBoolean requested = new AtomicBoolean(false);
      final AtomicBoolean rejected = new AtomicBoolean(false);
      final AtomicBoolean succeeded = new AtomicBoolean(false);

      final CountDownLatch latch = new CountDownLatch(3);

      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstGaveUpSecond: pin rejected");
              rejected.set(true);
              return Option.none();
            } finally {
              latch.countDown();
            }
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstGaveUpSecond: pin requested");
              requested.set(true);
              return Option.some(new AccountPIN("wrong"));
            } finally {
              latch.countDown();
            }
          }
        };

      final AccountLoginListenerType listener =
        new AccountLoginListenerType() {
          @Override public void onAccountLoginFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              System.out
                .println("testBooksLoginRejectedFirstGaveUpSecond: failed to log in");
              succeeded.set(false);
            } finally {
              latch.countDown();
            }
          }

          @Override public void onAccountLoginSuccess(
            final AccountBarcode used)
          {
            latch.countDown();
          }
        };

      b.accountLogin(barcode, pin_listener, listener);

      latch.await();
      TestUtilities.assertEquals(requested.get(), true);
      TestUtilities.assertEquals(rejected.get(), true);
      TestUtilities.assertEquals(succeeded.get(), false);

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

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);
      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            return Option.some(pin);
          }
        };

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
            final AccountBarcode actual)
          {
            try {
              System.err
                .println("testBooksSyncFileNotDirectory: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, login_listener);

      latch0.await();

      final File data = new File(tmp, "data");
      new File(data, "barcode.txt").delete();
      new File(data, "pin.txt").delete();
      data.delete();
      tmp.delete();
      tmp.createNewFile();

      TestUtilities.assertTrue(tmp.isFile());

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean failed = new AtomicBoolean(false);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {
          @Override public void onAccountSyncBook(
            final Book book)
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

      b.accountSync(pin_listener, sync_listener);
      latch1.await();
      TestUtilities.assertEquals(failed.get(), true);

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

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);
      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            return Option.some(pin);
          }
        };

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
            final AccountBarcode actual)
          {
            try {
              System.err.println("testBooksSyncOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, login_listener);

      latch0.await();

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {
          @Override public void onAccountSyncBook(
            final Book book)
          {
            count.incrementAndGet();
          }

          @Override public void onAccountSyncFailure(
            final OptionType<Throwable> error,
            final String message)
          {
            try {
              ok.set(false);
              System.err.println("testBooksSyncOK: login failed: " + message);
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

      b.accountSync(pin_listener, sync_listener);
      latch1.await();

      TestUtilities.assertEquals(ok.get(), true);
      TestUtilities.assertEquals(count.get(), 4);

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

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(tmp);
      bcb.setLoansURI(BooksContract.LOANS_URI);

      final BooksConfiguration in_config = bcb.build();
      final AccountBarcode barcode = new AccountBarcode("barcode");
      final AccountPIN pin = new AccountPIN("pin");
      final HTTPType in_http = BooksContract.makeAuthHTTP(barcode, pin);
      final BooksType b =
        Books.newBooks(exec, OPDSFeedParser.newParser(), in_http, in_config);

      final CountDownLatch latch0 = new CountDownLatch(1);
      final AccountPINListenerType pin_listener =
        new AccountPINListenerType() {
          @Override public OptionType<AccountPIN> onAccountPINRejected()
          {
            return Option.some(pin);
          }

          @Override public OptionType<AccountPIN> onAccountPINRequested()
          {
            return Option.some(pin);
          }
        };

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
            final AccountBarcode actual)
          {
            try {
              System.err.println("testBooksSyncLoadOK: login succeeded");
            } finally {
              latch0.countDown();
            }
          }
        };

      b.accountLogin(barcode, pin_listener, login_listener);

      latch0.await();

      final CountDownLatch latch1 = new CountDownLatch(1);
      final AtomicBoolean ok = new AtomicBoolean(false);
      final AtomicInteger count = new AtomicInteger(0);

      final AccountSyncListenerType sync_listener =
        new AccountSyncListenerType() {
          @Override public void onAccountSyncBook(
            final Book book)
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

      b.accountSync(pin_listener, sync_listener);
      latch1.await();

      TestUtilities.assertEquals(ok.get(), true);
      TestUtilities.assertEquals(count.get(), 4);

      ok.set(false);
      count.set(0);
      final CountDownLatch latch2 = new CountDownLatch(4);

      final AccountDataLoadListenerType load_listener =
        new AccountDataLoadListenerType() {
          @Override public void onAccountUnavailable()
          {
            ok.set(false);
          }

          @Override public void onAccountDataBookLoadSucceeded(
            final Book book)
          {
            try {
              count.incrementAndGet();
              ok.set(true);
            } finally {
              latch2.countDown();
            }
          }

          @Override public void onAccountDataBookLoadFailed(
            final BookID id,
            final OptionType<Throwable> error,
            final String message)
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
          @Override public void onAccountLogoutSuccess()
          {
            latch3.countDown();
          }

          @Override public void onAccountLogoutFailure(
            final OptionType<Throwable> error,
            final String message)
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
}
