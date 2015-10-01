package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

final class BooksControllerLogoutTask implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerLogoutTask.class));
  }

  private final AccountLogoutListenerType           listener;
  private final AtomicReference<AccountCredentials> login;
  private final OptionType<AdobeAdeptExecutorType>  adobe_drm;
  private final BookDatabaseType                    database;

  BooksControllerLogoutTask(
    final BookDatabaseType in_book_database,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AtomicReference<AccountCredentials> in_login,
    final AccountLogoutListenerType in_listener)
  {
    this.database = NullCheck.notNull(in_book_database);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.login = NullCheck.notNull(in_login);
    this.listener = new AccountLogoutListenerCatcher(
      BooksControllerLogoutTask.LOG, NullCheck.notNull(in_listener));
  }

  @Override public void run()
  {
    try {
      this.login.set(null);

      /**
       * Discard any device activations.
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
            }
          });
      }

      /**
       * Delete the books database.
       */

      this.database.databaseDestroy();

      this.listener.onAccountLogoutSuccess();
    } catch (final Throwable e) {
      this.listener.onAccountLogoutFailure(
        Option.some(e), NullCheck.notNull(e.getMessage()));
    }
  }
}
