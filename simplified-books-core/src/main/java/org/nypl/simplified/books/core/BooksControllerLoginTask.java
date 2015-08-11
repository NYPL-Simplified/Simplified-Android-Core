package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({ "boxing", "synthetic-access" })
final class BooksControllerLoginTask
  implements Runnable, AccountDataSetupListenerType
{
  private final AccountBarcode                                    barcode;
  private final BooksController                                   books;
  private final BookDatabaseType
                                                                  books_database;
  private final BooksControllerConfiguration                      config;
  private final HTTPType                                          http;
  private final AccountLoginListenerType                          listener;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;
  private final AccountPIN                                        pin;

  BooksControllerLoginTask(
    final BooksController in_books,
    final BookDatabaseType in_books_database,
    final HTTPType in_http,
    final BooksControllerConfiguration in_config,
    final AccountBarcode in_barcode,
    final AccountPIN in_pin,
    final AccountLoginListenerType in_listener,
    final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login)
  {
    this.books = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.barcode = NullCheck.notNull(in_barcode);
    this.pin = NullCheck.notNull(in_pin);
    this.listener = NullCheck.notNull(in_listener);
    this.login = NullCheck.notNull(in_login);
  }

  private void loginCheckCredentials()
    throws Exception
  {
    final HTTPAuthType auth =
      new HTTPAuthBasic(this.barcode.toString(), this.pin.toString());
    final HTTPResultType<Unit> r =
      this.http.head(Option.some(auth), this.config.getLoansURI());

    final BooksControllerLoginTask task = this;
    r.matchResult(
      new ResultAuthExceptional<Unit>()
      {
        @Override public HTTPResultOKType<Unit> onHTTPOK(
          final HTTPResultOKType<Unit> e)
          throws Exception
        {
          /**
           * Credentials were accepted, write them to files.
           */

          task.saveCredentials(task.pin);
          task.login.set(Pair.pair(task.barcode, task.pin));
          return e;
        }
      });
  }

  @Override public void onAccountDataSetupFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    this.listener.onAccountLoginFailure(error, message);
  }

  @Override public void onAccountDataSetupSuccess()
  {
    try {
      this.loginCheckCredentials();
      this.listener.onAccountLoginSuccess(this.barcode, this.pin);
    } catch (final Throwable e) {
      this.listener.onAccountLoginFailure(
        Option.some(e), NullCheck.notNull(e.getMessage()));
    }
  }

  @Override public void run()
  {
    this.books.submitRunnable(
      new BooksControllerDataSetupTask(
        this.books_database, this));
  }

  private void saveCredentials(
    final AccountPIN actual_pin)
    throws IOException
  {
    this.books_database.credentialsSet(this.barcode, actual_pin);
  }

  private static abstract class ResultAuthExceptional<A>
    implements HTTPResultMatcherType<A, HTTPResultOKType<A>, Exception>
  {
    public ResultAuthExceptional()
    {

    }

    @Override public final HTTPResultOKType<A> onHTTPError(
      final HTTPResultError<A> e)
      throws Exception
    {
      final String m = NullCheck.notNull(
        String.format(
          "%d: %s", e.getStatus(), e.getMessage()));

      switch (e.getStatus()) {
        case HttpURLConnection.HTTP_UNAUTHORIZED: {
          throw new AccountAuthenticationPINRejectedError(
            "Invalid barcode or PIN");
        }
        default: {
          throw new IOException(m);
        }
      }
    }

    @Override public final HTTPResultOKType<A> onHTTPException(
      final HTTPResultException<A> e)
      throws Exception
    {
      throw e.getError();
    }
  }
}
