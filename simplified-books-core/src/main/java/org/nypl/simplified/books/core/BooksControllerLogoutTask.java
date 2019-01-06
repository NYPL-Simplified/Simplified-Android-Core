package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.Callable;

final class BooksControllerLogoutTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerLogoutTask.class);

  private final AccountLogoutListenerType listener;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final BookDatabaseType database;
  private final AccountsDatabaseType accounts_database;
  private final AccountCredentials credentials;


  BooksControllerLogoutTask(
    final BookDatabaseType in_book_database,
    final AccountsDatabaseType in_accounts_database,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountLogoutListenerType in_listener,
    final BooksControllerConfigurationType in_config,
    final HTTPType in_http,
    final AccountCredentials in_credentials) {

    this.database =
      NullCheck.notNull(in_book_database);
    this.adobe_drm =
      NullCheck.notNull(in_adobe_drm);
    this.accounts_database =
      NullCheck.notNull(in_accounts_database);
    this.listener =
      new AccountLogoutListenerCatcher(LOG, NullCheck.notNull(in_listener));
    this.credentials =
      NullCheck.notNull(in_credentials);
  }

  private Unit deactivateDevice() throws Exception {

    /**
     * If an Adobe DRM implementation is available, activate the device
     * with the credentials. If the Adobe server rejects the credentials,
     * then the login attempt is still considered to have failed.
     */

    if (this.adobe_drm.isSome() && this.credentials.getAdobeUserID().isSome()) {

      BooksControllerDeviceDeActivationTask device_deactivation_task =
        new BooksControllerDeviceDeActivationTask(
          this.adobe_drm, this.credentials, this.accounts_database) {

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
      device_deactivation_task.call();

    } else {

      /**
       * Otherwise, the login process is completed.
       */

      this.onDeactivationSucceeded();
    }
    return Unit.unit();
  }

  public void onDeactivationSucceeded() {

    /*
     * Delete the books database.
     */

    try {
      new DeviceManagerDeleteTask(this.credentials).run();

      this.accounts_database.accountRemoveCredentials();
      this.database.databaseDestroy();
      this.listener.onAccountLogoutSuccess();
    } catch (IOException e) {
      LOG.error("deactivation failed: ", e);
      this.listener.onAccountLogoutFailure(Option.<Throwable>some(e), e.getMessage());
    }
  }

  @Override
  public Unit call() throws Exception {
    return this.deactivateDevice();
  }
}
