package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptActivationReceiverType;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.http.core.HTTPAuthBasic;
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

final class BooksControllerLoginTask implements Runnable,
  AccountDataSetupListenerType,
  AdobeAdeptActivationReceiverType
{
  private static final Logger        LOG;
  private static final AdobeVendorID VENDOR_ID;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerLoginTask.class));
    VENDOR_ID = new AdobeVendorID("NYPL");
  }

  private final AccountBarcode                                    barcode;
  private final BooksController                                   books;
  private final BookDatabaseType
                                                                  books_database;
  private final BooksControllerConfigurationType                  config;
  private final HTTPType                                          http;
  private final AccountLoginListenerType                          listener;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;
  private final AccountPIN                                        pin;
  private final OptionType<AdobeAdeptExecutorType>                adobe_drm;

  BooksControllerLoginTask(
    final BooksController in_books,
    final BookDatabaseType in_books_database,
    final HTTPType in_http,
    final BooksControllerConfigurationType in_config,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountBarcode in_barcode,
    final AccountPIN in_pin,
    final AccountLoginListenerType in_listener,
    final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login)
  {
    this.books = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.barcode = NullCheck.notNull(in_barcode);
    this.pin = NullCheck.notNull(in_pin);
    this.listener = new AccountLoginListenerCatcher(
      BooksControllerLoginTask.LOG, NullCheck.notNull(in_listener));
    this.login = NullCheck.notNull(in_login);
  }

  @Override public void onAccountDataSetupFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    this.listener.onAccountLoginFailureLocalError(error, message);
  }

  @Override public void onAccountDataSetupSuccess()
  {
    /**
     * Setting up the database was successful, now try hitting the remote
     * server and seeing whether or not it rejects the given credentials.
     */

    final URI loans_uri = this.config.getCurrentLoansURI();

    BooksControllerLoginTask.LOG.debug(
      "attempting login on {}", loans_uri);

    final HTTPAuthType auth =
      new HTTPAuthBasic(this.barcode.toString(), this.pin.toString());
    final HTTPResultType<Unit> r = this.http.head(Option.some(auth), loans_uri);

    r.matchResult(
      new HTTPResultMatcherType<Unit, Unit, UnreachableCodeException>()
      {
        @Override public Unit onHTTPError(final HTTPResultError<Unit> e)
        {
          BooksControllerLoginTask.this.onHTTPServerReturnedError(e);
          return Unit.unit();
        }

        @Override public Unit onHTTPException(final HTTPResultException<Unit> e)
        {
          BooksControllerLoginTask.this.onHTTPException(e);
          return Unit.unit();
        }

        @Override public Unit onHTTPOK(final HTTPResultOKType<Unit> e)
        {
          BooksControllerLoginTask.this.onHTTPServerAcceptedCredentials();
          return Unit.unit();
        }
      });
  }

  private <T> void onHTTPException(final HTTPResultException<T> e)
  {
    final Exception ex = e.getError();
    this.listener.onAccountLoginFailureLocalError(
      Option.some((Throwable) ex), ex.getMessage());
  }

  private void onHTTPServerReturnedError(final HTTPResultError<Unit> e)
  {
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

  private void onHTTPServerAcceptedCredentials()
  {
    /**
     * If an Adobe DRM implementation is available, activate the device
     * with the credentials. If the Adobe server rejects the credentials,
     * then the login attempt is still considered to have failed.
     */

    if (this.adobe_drm.isSome()) {
      final Some<AdobeAdeptExecutorType> some =
        (Some<AdobeAdeptExecutorType>) this.adobe_drm;
      final AdobeAdeptExecutorType adobe = some.get();
      adobe.execute(
        new AdobeAdeptProcedureType()
        {
          @Override public void executeWith(final AdobeAdeptConnectorType c)
          {
            c.discardDeviceActivations();
            c.activateDevice(
              BooksControllerLoginTask.this,
              BooksControllerLoginTask.VENDOR_ID,
              BooksControllerLoginTask.this.barcode.toString(),
              BooksControllerLoginTask.this.pin.toString());
          }
        });
    } else {

      /**
       * Otherwise, the login process is completed.
       */

      this.onCompletedSuccessfully();
    }
  }

  private void onCompletedSuccessfully()
  {
    try {
      this.books_database.credentialsSet(this.barcode, this.pin);
      this.login.set(Pair.pair(this.barcode, this.pin));
    } catch (final IOException e) {
      this.listener.onAccountLoginFailureLocalError(
        Option.some((Throwable) e), e.getMessage());
      return;
    }

    BooksControllerLoginTask.LOG.debug(
      "logged in as {} successfully", this.barcode);

    this.listener.onAccountLoginSuccess(this.barcode, this.pin);
  }

  @Override public void run()
  {
    /**
     * Set up the initial data directories, and notify this task
     * via the {@link AccountDataSetupListenerType} interface upon
     * success or failure.
     */

    this.books.submitRunnable(
      new BooksControllerDataSetupTask(this.books_database, this));
  }

  @Override public void onActivationsCount(final int count)
  {
    /**
     * Device activation succeeded.
     */

    this.onCompletedSuccessfully();
  }

  @Override public void onActivation(
    final int index,
    final AdobeVendorID authority,
    final String device_id,
    final String user_name,
    final AdobeUserID user_id,
    final String expires)
  {
    BooksControllerLoginTask.LOG.debug(
      "Activation [{}]: authority: {}", Integer.valueOf(index), authority);
    BooksControllerLoginTask.LOG.debug(
      "Activation [{}]: device_id: {}", Integer.valueOf(index), device_id);
    BooksControllerLoginTask.LOG.debug(
      "Activation [{}]: user_name: {}", Integer.valueOf(index), user_name);
    BooksControllerLoginTask.LOG.debug(
      "Activation [{}]: user_id: {}", Integer.valueOf(index), user_id);
    BooksControllerLoginTask.LOG.debug(
      "Activation [{}]: expires: {}", Integer.valueOf(index), expires);
  }

  @Override public void onActivationError(final String message)
  {
    this.listener.onAccountLoginFailureDeviceActivationError(message);
  }
}
