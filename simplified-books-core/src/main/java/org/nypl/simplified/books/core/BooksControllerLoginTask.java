package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
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
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class BooksControllerLoginTask implements Runnable,
  AccountDataSetupListenerType,
  AdobeAdeptActivationReceiverType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerLoginTask.class));
  }

  private final BooksController                     books;
  private final BookDatabaseType                    books_database;
  private final BooksControllerConfigurationType    config;
  private final HTTPType                            http;
  private final AccountLoginListenerType            listener;
  private final AtomicReference<AccountCredentials> login;
  private final OptionType<AdobeAdeptExecutorType>  adobe_drm;
  private final AccountCredentials                  credentials;
  private final OPDSFeedParserType                  parser;
  private final DownloaderType                      downloader;
  private final AtomicBoolean                       syncing;

  BooksControllerLoginTask(
    final BooksController in_books,
    final BookDatabaseType in_books_database,
    final HTTPType in_http,
    final BooksControllerConfigurationType in_config,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final OPDSFeedParserType in_feed_parser,
    final DownloaderType in_downloader,
    final AccountCredentials in_credentials,
    final AccountLoginListenerType in_listener,
    final AtomicReference<AccountCredentials> in_login,
    final AtomicBoolean in_syncing)
  {
    this.books = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.parser = NullCheck.notNull(in_feed_parser);
    this.downloader = NullCheck.notNull(in_downloader);
    this.credentials = NullCheck.notNull(in_credentials);
    this.listener = new AccountLoginListenerCatcher(
      BooksControllerLoginTask.LOG, NullCheck.notNull(in_listener));
    this.login = NullCheck.notNull(in_login);
    this.syncing = NullCheck.notNull(in_syncing);
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

    final AccountBarcode user = this.credentials.getUser();
    final AccountPIN pass = this.credentials.getPassword();
    final HTTPAuthType auth =
      new HTTPAuthBasic(user.toString(), pass.toString());
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
      final AdobeAdeptExecutorType adobe_exec = some.get();

      final AccountBarcode user = this.credentials.getUser();
      final AccountPIN pass = this.credentials.getPassword();
      final OptionType<AdobeVendorID> vendor_opt =
        this.credentials.getAdobeVendor();

      vendor_opt.accept(
        new OptionVisitorType<AdobeVendorID, Unit>()
        {
          @Override public Unit none(final None<AdobeVendorID> n)
          {
            BooksControllerLoginTask.this.onActivationError(
              "No Adobe vendor ID provided");
            return Unit.unit();
          }

          @Override public Unit some(final Some<AdobeVendorID> s)
          {
            adobe_exec.execute(
              new AdobeAdeptProcedureType()
              {
                @Override
                public void executeWith(final AdobeAdeptConnectorType c)
                {
                  c.discardDeviceActivations();
                  c.activateDevice(
                    BooksControllerLoginTask.this,
                    s.get(),
                    user.toString(),
                    pass.toString());
                }
              });
            return Unit.unit();
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
      this.books_database.databaseAccountCredentialsSet(this.credentials);
      this.login.set(this.credentials);
    } catch (final IOException e) {
      this.listener.onAccountLoginFailureLocalError(
        Option.some((Throwable) e), e.getMessage());
      return;
    }

    BooksControllerLoginTask.LOG.debug(
      "logged in as {} successfully", this.credentials.getUser());

    try {
      final BooksControllerSyncTask sync = new BooksControllerSyncTask(
        this.books,
        this.books_database,
        this.config,
        this.http,
        this.parser,
        this.listener,
        this.syncing);
      sync.run();
    } catch (final Throwable e) {
      BooksControllerLoginTask.LOG.debug("sync task raised error: ", e);
    }

    this.listener.onAccountLoginSuccess(this.credentials);
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
