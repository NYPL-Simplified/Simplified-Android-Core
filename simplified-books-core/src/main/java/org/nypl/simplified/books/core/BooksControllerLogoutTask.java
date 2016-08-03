package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
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
import java.net.URI;
import java.util.Scanner;

final class BooksControllerLogoutTask implements Runnable {
  private static final Logger LOG;

  static {
    LOG =  LogUtilities.getLog(BooksControllerLogoutTask.class);
  }

  private final AccountLogoutListenerType listener;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final BookDatabaseType database;
  private final AccountsDatabaseType accounts_database;
  private final BooksControllerConfigurationType config;
  private final HTTPType http;
  private final AccountCredentials credentials;


  BooksControllerLogoutTask(
    final BookDatabaseType in_book_database,
    final AccountsDatabaseType in_accounts_database,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountLogoutListenerType in_listener,
    final BooksControllerConfigurationType in_config,
    final HTTPType in_http,
    final AccountCredentials in_credentials) {
    this.database = NullCheck.notNull(in_book_database);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.listener = new AccountLogoutListenerCatcher(
      BooksControllerLogoutTask.LOG, NullCheck.notNull(in_listener));
    this.config = NullCheck.notNull(in_config);
    this.http = NullCheck.notNull(in_http);
    this.credentials = NullCheck.notNull(in_credentials);

  }


  private <T> void onHTTPException(final HTTPResultException<T> e) {
    final Exception ex = e.getError();
    BooksControllerLogoutTask.this.listener.onAccountLogoutFailure(Option.<Throwable>some(ex), ex.getMessage());
  }

  private void onHTTPServerReturnedError(final HTTPResultError<?> e) {
    final int code = e.getStatus();
    BooksControllerLogoutTask.this.listener.onAccountLogoutFailureServerError(code);
  }

  private void onHTTPServerAcceptedCredentials(final InputStream data) {
    /**
     * If an Adobe DRM implementation is available, activate the device
     * with the credentials. If the Adobe server rejects the credentials,
     * then the login attempt is still considered to have failed.
     */

    if (this.adobe_drm.isSome()) {

/* remove this , once permanent token is implemented*/
      Scanner scanner = new Scanner(data).useDelimiter("\\A");
      String adobe_token = scanner.hasNext() ? scanner.next() : "";
      BooksControllerLogoutTask.LOG.debug("adobe temporary token: {}", adobe_token);
      this.credentials.setAdobeToken(Option.some(new AccountAdobeToken(adobe_token)));
/**/

      BooksControllerDeviceDeActivationTask device_deactivationTask = new BooksControllerDeviceDeActivationTask(this.adobe_drm,
        this.credentials) {

        @Override
        public void onDeactivationError(final String message) {
          BooksControllerLogoutTask.this.listener.onAccountLogoutFailure(Option.<Throwable>none(), message);
        }

        @Override
        public void onDeactivationSucceeded() {
          /**
           * Delete the books database.
           */

          BooksControllerLogoutTask.this.onDeactivationSucceeded();

        }

      };
      device_deactivationTask.run();

    } else {

      /**
       * Otherwise, the login process is completed.
       */

      this.onDeactivationSucceeded();
    }
  }

  public void onDeactivationSucceeded() {
    /**
     * Delete the books database.
     */

    try {
      BooksControllerLogoutTask.this.accounts_database.accountRemoveCredentials();
      BooksControllerLogoutTask.this.database.databaseDestroy();
      BooksControllerLogoutTask.this.listener.onAccountLogoutSuccess();
    } catch (IOException e) {
      e.printStackTrace();
      BooksControllerLogoutTask.this.listener.onAccountLogoutFailure(Option.<Throwable>some(e), e.getMessage());
    }
  }


  @Override
  public void run() {


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

    BooksControllerLogoutTask.LOG.debug(
      "attempting login on {}", auth_uri);


    r.matchResult(
      new HTTPResultMatcherType<InputStream, Unit, UnreachableCodeException>() {
        @Override
        public Unit onHTTPError(final HTTPResultError<InputStream> e) {
          BooksControllerLogoutTask.this.onHTTPServerReturnedError(e);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPException(final HTTPResultException<InputStream> e) {
          BooksControllerLogoutTask.this.onHTTPException(e);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPOK(final HTTPResultOKType<InputStream> e) {
          BooksControllerLogoutTask.this.onHTTPServerAcceptedCredentials(e.getValue());
          return Unit.unit();
        }
      });


  }
}
