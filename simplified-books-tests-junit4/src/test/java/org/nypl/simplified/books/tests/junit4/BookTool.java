package org.nypl.simplified.books.tests.junit4;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountPINListenerType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsType;
import org.nypl.simplified.books.core.Book;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.Books;
import org.nypl.simplified.books.core.BooksConfiguration;
import org.nypl.simplified.books.core.BooksConfigurationBuilderType;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

public final class BookTool
{
  public static void main(
    final String args[])
  {
    final ExecutorService exec =
      NullCheck.notNull(Executors.newFixedThreadPool(4));
    final HTTPType http = HTTP.newHTTP();

    final OPDSFeedParserType parser = OPDSFeedParser.newParser();

    final BooksConfigurationBuilderType books_config_builder =
      BooksConfiguration.newBuilder(new File("/tmp/books"));

    books_config_builder
      .setLoansURI(URI
        .create("http://10.1.3.1:9999/org/nypl/simplified/downloader/tests/loans.xml"));

    final DownloaderType d =
      Downloader.newDownloader(exec, http, DownloaderConfiguration
        .newBuilder(new File("/tmp/downloads"))
        .build());

    final BooksConfiguration books_config = books_config_builder.build();
    final AccountsType books =
      Books.newBooks(exec, parser, http, d, books_config);

    final AccountBarcode barcode = new AccountBarcode("4545499");
    final AccountPIN pin = new AccountPIN("4444");

    final AccountPINListenerType pin_listener = new AccountPINListenerType() {
      @Override public OptionType<AccountPIN> onAccountPINRequested()
      {
        System.err.println("info: pin requested");
        return Option.some(pin);
      }

      @Override public OptionType<AccountPIN> onAccountPINRejected()
      {
        System.err.println("info: pin rejected");
        return Option.none();
      }
    };

    System.err.println("info: loading books, if any");
    books.accountLoadBooks(new AccountDataLoadListenerType() {
      @Override public void onAccountDataBookLoadSucceeded(
        final Book book)
      {
        System.err.println("info: account-load: loaded book: " + book.getID());
      }

      @Override public void onAccountDataBookLoadFailed(
        final BookID id,
        final OptionType<Throwable> error,
        final String message)
      {
        System.err.println("error: account-load: failed to load book: "
          + id
          + ": "
          + message);
        if (error.isSome()) {
          final Some<Throwable> some = (Some<Throwable>) error;
          some.get().printStackTrace();
        }
      }

      @Override public void onAccountUnavailable()
      {
        System.err.println("info: account-load: not logged in");
      }
    });

    final AccountLogoutListenerType logout_listener =
      new AccountLogoutListenerType() {
        @Override public void onAccountLogoutSuccess()
        {
          System.err.println("info: account-logout: logged out");
          exec.shutdown();
        }

        @Override public void onAccountLogoutFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println("info: account-logout: failed to log out: "
            + message);
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            some.get().printStackTrace();
          }

          exec.shutdown();
        }
      };

    final AccountSyncListenerType sync_listener =
      new AccountSyncListenerType() {
        @Override public void onAccountSyncSuccess()
        {
          System.err.println("info: account-sync: synced books");
          System.err.println("info: account-sync: logging out");
          books.accountLogout(logout_listener);
        }

        @Override public void onAccountSyncFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println("error: account-sync: could not sync books: "
            + message);
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            some.get().printStackTrace();
          }
        }

        @Override public void onAccountSyncBook(
          final Book book)
        {
          System.err.println("info: account-sync: synced book "
            + book.getID());
        }
      };

    final AccountLoginListenerType login_listener =
      new AccountLoginListenerType() {
        @Override public void onAccountLoginSuccess(
          final AccountBarcode b)
        {
          System.err.println("info: account-login: logged in");
          System.err.println("info: account-login: sync requested");
          books.accountSync(pin_listener, sync_listener);
        }

        @Override public void onAccountLoginFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println("error: account-login: could not log in: "
            + message);
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            some.get().printStackTrace();
          }
        }
      };

    books.accountLogin(barcode, pin_listener, login_listener);
  }
}
