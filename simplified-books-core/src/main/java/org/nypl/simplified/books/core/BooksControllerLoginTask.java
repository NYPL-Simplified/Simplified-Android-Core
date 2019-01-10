package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

final class BooksControllerLoginTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerLoginTask.class);

  private final BooksController books;
  private final BookDatabaseType books_database;
  private final BooksControllerConfigurationType config;
  private final HTTPType http;
  private final AccountLoginListenerType listener;
  private final DeviceActivationListenerType device_listener;
  private final AccountCredentials credentials;
  private final AccountsDatabaseType accounts_database;

  BooksControllerLoginTask(
    final BooksController in_books,
    final BookDatabaseType in_books_database,
    final AccountsDatabaseType in_accounts_database,
    final HTTPType in_http,
    final BooksControllerConfigurationType in_config,
    final AccountCredentials in_credentials,
    final AccountLoginListenerType in_listener,
    final DeviceActivationListenerType in_device_listener) {
    this.books = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.credentials = NullCheck.notNull(in_credentials);
    this.listener = new AccountLoginListenerCatcher(LOG, NullCheck.notNull(in_listener));
    this.device_listener = in_device_listener;
  }

  private <T> void onHTTPException(final HTTPResultException<T> e) {
    final Exception ex = e.getError();
    this.listener.onAccountLoginFailureLocalError(Option.some(ex), ex.getMessage());
  }

  private void onHTTPServerReturnedError(final HTTPResultError<?> e) {
    final int code = e.getStatus();
    switch (code) {
      case HttpURLConnection.HTTP_UNAUTHORIZED: {
        this.listener.onAccountLoginFailureCredentialsIncorrect();
        break;
      }
      default: {
        this.listener.onAccountLoginFailureServerError(code);
      }
    }
  }

  private void onCompletedSuccessfully() {
    this.listener.onAccountLoginSuccess(this.credentials);

    LOG.debug("logged in as {} successfully", this.credentials.getBarcode());

    try {
      this.accounts_database.accountSetCredentials(this.credentials);
    } catch (final IOException e) {
      LOG.error("could not save credentials: ", e);
      this.listener.onAccountLoginFailureLocalError(Option.some(e), e.getMessage());
    }
  }

  @Override
  public Unit call() throws Exception {

    /*
     * Set up the data directory.
     */

    new BooksControllerDataSetupTask(
      this.books_database,
      new AccountDataSetupListenerType() {
        @Override
        public void onAccountDataSetupFailure(OptionType<Throwable> error, String message) {
          listener.onAccountLoginFailureLocalError(error, message);
        }

        @Override
        public void onAccountDataSetupSuccess() {

        }
      }).call();

    /*
     * Setting up the database was successful, now try hitting the remote
     * server and seeing whether or not it rejects the given credentials.
     */

    final HTTPAuthType http_auth =
      AccountCredentialsHTTP.Companion.toHttpAuth(this.credentials);

    final URI auth_uri =
      this.config.getCurrentRootFeedURI().resolve("loans/");

    final HTTPResultType<InputStream> r = this.http.head(Option.some(http_auth), auth_uri);

    LOG.debug("attempting login on {}", auth_uri);

    return r.matchResult(
      new HTTPResultMatcherType<InputStream, Unit, UnreachableCodeException>() {
        @Override
        public Unit onHTTPError(final HTTPResultError<InputStream> e) {
          BooksControllerLoginTask.this.onHTTPServerReturnedError(e);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPException(final HTTPResultException<InputStream> e) {
          BooksControllerLoginTask.this.onHTTPException(e);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPOK(final HTTPResultOKType<InputStream> e) {
          BooksControllerLoginTask.this.onCompletedSuccessfully();
          return Unit.unit();
        }
      });
  }
}
