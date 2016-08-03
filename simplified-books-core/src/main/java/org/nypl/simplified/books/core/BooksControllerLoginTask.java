package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.*;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

final class BooksControllerLoginTask implements Runnable,
  AccountDataSetupListenerType {
  private static final Logger LOG;

  static {
    LOG =  LogUtilities.getLog(BooksControllerLoginTask.class);
  }

  private final BooksController books;
  private final BookDatabaseType books_database;
  private final BooksControllerConfigurationType config;
  private final HTTPType http;
  private final AccountLoginListenerType listener;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final AccountCredentials credentials;
  private final OPDSFeedParserType parser;
  private final AtomicBoolean syncing;
  private final AccountsDatabaseType accounts_database;

  BooksControllerLoginTask(
    final BooksController in_books,
    final BookDatabaseType in_books_database,
    final AccountsDatabaseType in_accounts_database,
    final HTTPType in_http,
    final BooksControllerConfigurationType in_config,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final OPDSFeedParserType in_feed_parser,
    final AccountCredentials in_credentials,
    final AccountLoginListenerType in_listener,
    final AtomicBoolean in_syncing) {
    this.books = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.parser = NullCheck.notNull(in_feed_parser);
    this.credentials = NullCheck.notNull(in_credentials);
    this.listener = new AccountLoginListenerCatcher(
      BooksControllerLoginTask.LOG, NullCheck.notNull(in_listener));
    this.syncing = NullCheck.notNull(in_syncing);
  }

  @Override
  public void onAccountDataSetupFailure(
    final OptionType<Throwable> error,
    final String message) {
    this.listener.onAccountLoginFailureLocalError(error, message);
  }

  @Override
  public void onAccountDataSetupSuccess() {
    /**
     * Setting up the database was successful, now try hitting the remote
     * server and seeing whether or not it rejects the given credentials.
     */


    final AccountBarcode user = this.credentials.getUser();
    final AccountPIN pass = this.credentials.getPassword();
    HTTPAuthType auth =
      new HTTPAuthBasic(user.toString(), pass.toString());

    if (credentials.getAuthToken().isSome()) {
      final AccountAuthToken token = ((Some<AccountAuthToken>) credentials.getAuthToken()).get();
      if (token != null) {
        auth = new HTTPAuthOAuth(token.toString());
      }
    }

    URI auth_uri = this.config.getCurrentLoansURI();
    HTTPResultType<InputStream> r;
    if (this.adobe_drm.isSome()) {
      auth_uri = this.config.getCurrentRootFeedURI().resolve("AdobeAuth/authdata");
      r = this.http.get(Option.some(auth), auth_uri, 0);
    } else {
      r = this.http.head(Option.some(auth), auth_uri);
    }

    BooksControllerLoginTask.LOG.debug(
      "attempting login on {}", auth_uri);


    r.matchResult(
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
          BooksControllerLoginTask.this.onHTTPServerAcceptedCredentials(e.getValue());
          return Unit.unit();
        }
      });
  }

  private <T> void onHTTPException(final HTTPResultException<T> e) {
    final Exception ex = e.getError();
    this.listener.onAccountLoginFailureLocalError(
      Option.some((Throwable) ex), ex.getMessage());
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

  private void onHTTPServerAcceptedCredentials(final InputStream data) {
    /**
     * If an Adobe DRM implementation is available, activate the device
     * with the credentials. If the Adobe server rejects the credentials,
     * then the login attempt is still considered to have failed.
     */

    if (this.adobe_drm.isSome()) {


      Scanner scanner = new Scanner(data).useDelimiter("\\A");
      String adobe_token = scanner.hasNext() ? scanner.next() : "";

      BooksControllerLoginTask.LOG.debug("adobe temporary token: {}", adobe_token);

      this.credentials.setAdobeToken(Option.some(new AccountAdobeToken(adobe_token)));

      BooksControllerDeviceActivationTask activation_task =
        new BooksControllerDeviceActivationTask(this.adobe_drm,
          this.credentials,
          this.accounts_database) {
          @Override
          public void onActivationsCount(final int count) {
            /**
             * Device activation succeeded.
             */

            BooksControllerLoginTask.this.onCompletedSuccessfully();
          }

          @Override
          public void onActivation(int index, AdobeVendorID authority, String device_id, String user_name, AdobeUserID user_id, String expires) {
            super.onActivation(index, authority, device_id, user_name, user_id, expires);

            AdobeDeviceID adobeDeviceID = new AdobeDeviceID(device_id);

            BooksControllerLoginTask.this.credentials.setAdobeDeviceID(Option.some(adobeDeviceID));
            BooksControllerLoginTask.this.credentials.setAdobeUserID(Option.some(user_id));
          }

          @Override
          public void onActivationError(final String message) {
            BooksControllerLoginTask.this.listener.onAccountLoginFailureDeviceActivationError(message);
          }
        };
      activation_task.run();

    } else {

      /**
       * Otherwise, the login process is completed.
       */

      this.onCompletedSuccessfully();
    }
  }

  private void onCompletedSuccessfully() {
    try {
      //fake invalid pin
      //this.credentials.setAdobePIN(new AccountPIN("bla"));

      this.accounts_database.accountSetCredentials(this.credentials);
    } catch (final IOException e) {
      BooksControllerLoginTask.LOG.error("could not save credentials: ", e);
      this.listener.onAccountLoginFailureLocalError(
        Option.some((Throwable) e), e.getMessage());
      return;
    }
    this.listener.onAccountLoginSuccess(this.credentials);

    BooksControllerLoginTask.LOG.debug(
      "logged in as {} successfully", this.credentials.getUser());

    try {
      final BooksControllerSyncTask sync = new BooksControllerSyncTask(
        this.books,
        this.books_database,
        this.accounts_database,
        this.config,
        this.http,
        this.parser,
        this.listener,
        this.syncing);
//      sync.run(); // was causing login to take forever
      new Thread(sync).run();
    } catch (final Throwable e) {
      BooksControllerLoginTask.LOG.debug("sync task raised error: ", e);
    }

  }

  @Override
  public void run() {
    /**
     * Set up the initial data directories, and notify this task
     * via the {@link AccountDataSetupListenerType} interface upon
     * success or failure.
     */

    this.books.submitRunnable(
      new BooksControllerDataSetupTask(this.books_database, this));
  }
}
