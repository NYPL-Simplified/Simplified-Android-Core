package org.nypl.simplified.books.tests.junit4;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfiguration;
import org.nypl.simplified.books.core.BooksControllerConfigurationBuilderType;
import org.nypl.simplified.books.core.FeedHTTPTransport;
import org.nypl.simplified.books.core.FeedLoader;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthType;
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

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BookTool
{
  private BookTool()
  {
    throw new UnreachableCodeException();
  }

  public static void main(
    final String args[])
  {
    final ExecutorService exec =
      NullCheck.notNull(Executors.newFixedThreadPool(4));
    final HTTPType http = HTTP.newHTTP();

    final OPDSFeedParserType parser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport =
      FeedHTTPTransport.newTransport(http);
    final OPDSSearchParserType in_search_parser = OPDSSearchParser.newParser();
    final FeedLoaderType in_loader =
      FeedLoader.newFeedLoader(exec, parser, in_transport, in_search_parser);

    final BooksControllerConfigurationBuilderType books_config_builder =
      BooksControllerConfiguration.newBuilder(new File("/tmp/books"));

    books_config_builder.setLoansURI(
      URI.create("http://circulation.alpha.librarysimplified.org/loans/"));

    final DownloaderType d =
      DownloaderHTTP.newDownloader(exec, new File("/tmp/downloader"), http);

    final OPDSJSONSerializerType in_json_serializer =
      OPDSJSONSerializer.newSerializer();
    final BooksControllerConfiguration books_config =
      books_config_builder.build();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

    final OptionType<AdobeAdeptExecutorType> none = Option.none();
    final AccountsType books = BooksController.newBooks(
      exec,
      in_loader,
      http,
      d,
      in_json_serializer,
      in_json_parser,
      books_config,
      none);

    final AccountBarcode barcode = new AccountBarcode("LABS00000010");
    final AccountPIN pin = new AccountPIN("3198");

    System.err.println("info: loading books, if any");
    books.accountLoadBooks(
      new AccountDataLoadListenerType()
      {

        @Override public void onAccountDataBookLoadFailed(
          final BookID id,
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println(
            "error: account-load: failed to load book: " + id + ": " + message);
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            some.get().printStackTrace();
          }
        }

        @Override public void onAccountUnavailable()
        {
          System.err.println("info: account-load: not logged in");

        }

        @Override public void onAccountDataBookLoadFinished()
        {
          // Nothing
        }

        @Override public void onAccountDataBookLoadSucceeded(
          final BookID book,
          final BookSnapshot snap)
        {
          System.err.println("info: account-load: loaded book: " + book);
        }

        @Override public void onAccountDataLoadFailedImmediately(
          final Throwable error)
        {
          System.err.println("error: account-load: failed to data: " + error);
          error.printStackTrace(System.err);
        }
      });

    final AccountLogoutListenerType logout_listener =
      new AccountLogoutListenerType()
      {
        @Override public void onAccountLogoutSuccess()
        {
          System.err.println("info: account-logout: logged out");
          exec.shutdown();
        }

        @Override public void onAccountLogoutFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println(
            "info: account-logout: failed to log out: " + message);
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            some.get().printStackTrace();
          }

          exec.shutdown();
        }
      };

    final AccountSyncListenerType sync_listener = new AccountSyncListenerType()
    {

      @Override public void onAccountSyncSuccess()
      {
        System.err.println("info: account-sync: synced books");
        System.err.println("info: account-sync: logging out");
        books.accountLogout(logout_listener);
      }

      @Override public void onAccountSyncBookDeleted(final BookID book)
      {
        System.err.println("info: account-sync: deleted book " + book);
      }

      @Override public void onAccountSyncFailure(
        final OptionType<Throwable> error,
        final String message)
      {
        System.err.println(
          "error: account-sync: could not sync books: " + message);
        if (error.isSome()) {
          final Some<Throwable> some = (Some<Throwable>) error;
          some.get().printStackTrace();
        }
      }

      @Override public void onAccountSyncAuthenticationFailure(
        final String message)
      {
        System.err.println(
          "error: account-sync: could not sync books: " + message);
      }

      @Override public void onAccountSyncBook(
        final BookID book)
      {
        System.err.println("info: account-sync: synced book " + book);
      }
    };

    final AccountLoginListenerType login_listener =
      new AccountLoginListenerType()
      {
        @Override public void onAccountLoginFailureCredentialsIncorrect()
        {
          System.err.println(
            "error: account-login: could not log in: credentials are "
            + "incorrect");
        }

        @Override public void onAccountLoginFailureServerError(final int code)
        {
          System.err.println(
            "error: account-login: could not log in: server error: " + code);
        }

        @Override public void onAccountLoginFailureLocalError(
          final OptionType<Throwable> error,
          final String message)
        {
          System.err.println(
            "error: account-login: could not log in: local exception");
          error.map(
            new FunctionType<Throwable, Unit>()
            {
              @Override public Unit call(final Throwable x)
              {
                x.printStackTrace();
                return Unit.unit();
              }
            });
        }

        @Override public void onAccountLoginSuccess(
          final AccountBarcode b,
          final AccountPIN p)
        {
          System.err.println("info: account-login: logged in");
          System.err.println("info: account-login: sync requested");
          books.accountSync(sync_listener);
        }

        @Override public void onAccountLoginFailureDeviceActivationError(
          final String message)
        {
          System.err.println(
            "error: account-login: could not log in: failed to activate "
            + "device: "
            + message);
        }
      };

    books.accountLogin(barcode, pin, login_listener);
  }
}
