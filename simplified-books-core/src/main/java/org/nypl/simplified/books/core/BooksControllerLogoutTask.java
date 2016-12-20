package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.IOException;

final class BooksControllerLogoutTask implements Runnable {
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(BooksControllerLogoutTask.class);
  }

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
    this.database = NullCheck.notNull(in_book_database);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.listener = new AccountLogoutListenerCatcher(
      BooksControllerLogoutTask.LOG, NullCheck.notNull(in_listener));
    this.credentials = NullCheck.notNull(in_credentials);

  }

  private void deactivateDevice() {
    /**
     * If an Adobe DRM implementation is available, activate the device
     * with the credentials. If the Adobe server rejects the credentials,
     * then the login attempt is still considered to have failed.
     */

    if (this.adobe_drm.isSome() && this.credentials.getAdobeUserID().isSome()) {

      BooksControllerDeviceDeActivationTask device_deactivation_task = new BooksControllerDeviceDeActivationTask(this.adobe_drm,
        this.credentials, this.accounts_database, this.database) {

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
      device_deactivation_task.run();

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

    BooksControllerLogoutTask.this.deactivateDevice();

  }
}
