package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeAdeptActivationReceiverType;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

final class BooksControllerLogoutTask implements Runnable,
  AdobeAdeptDeactivationReceiverType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerLogoutTask.class));
  }

  private final AccountLogoutListenerType          listener;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final BookDatabaseType                   database;
  private final AccountsDatabaseType               accounts_database;

  BooksControllerLogoutTask(
    final BookDatabaseType in_book_database,
    final AccountsDatabaseType in_accounts_database,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountLogoutListenerType in_listener)
  {
    this.database = NullCheck.notNull(in_book_database);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.listener = new AccountLogoutListenerCatcher(
      BooksControllerLogoutTask.LOG, NullCheck.notNull(in_listener));
  }

  @Override public void run()
  {
    try {

      final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
      if (this.adobe_drm.isSome() && credentials_opt.isSome()) {
        final AccountCredentials credentials = ((Some<AccountCredentials>) credentials_opt).get();
        final Some<AdobeAdeptExecutorType> some =
          (Some<AdobeAdeptExecutorType>) this.adobe_drm;
        final AccountBarcode user = credentials.getUser();
        final AccountPIN pass = credentials.getPassword();
        final OptionType<AdobeVendorID> vendor_opt = credentials.getAdobeVendor();
        final AdobeAdeptExecutorType adobe_exec = some.get();

        vendor_opt.accept(
          new OptionVisitorType<AdobeVendorID, Unit>()
          {
            @Override
            public Unit none(final None<AdobeVendorID> n)
            {
              BooksControllerLogoutTask.this.onDeactivationError(
                "No Adobe vendor ID provided");
              return Unit.unit();
            }

            @Override
            public Unit some(final Some<AdobeVendorID> s)
            {
              adobe_exec.execute(
                new AdobeAdeptProcedureType()
                {
                  @Override
                  public void executeWith(final AdobeAdeptConnectorType c)
                  {
                    final AdobeUserID[] user_id = new AdobeUserID[1];

                    c.getDeviceActivations(new AdobeAdeptActivationReceiverType()
                    {
                      @Override
                      public void onActivationsCount(final int count)
                      {
                        if (count == 0) {
                          BooksControllerLogoutTask.this.onDeactivationError("No devices were activated.");
                        }
                      }

                      @Override
                      public void onActivation(final int index,
                                               final AdobeVendorID authority,
                                               final String device_id,
                                               final String user_name,
                                               final AdobeUserID in_user_id,
                                               final String expires)
                      {
                        user_id[0] = in_user_id;
                      }

                      @Override
                      public void onActivationError(final String message)
                      {
                        BooksControllerLogoutTask.this.onDeactivationError("Activation error when getting existing activations. This should never happen.");
                      }
                    });

                    if (user_id[0] != null) {
                      c.deactivateDevice(
                        BooksControllerLogoutTask.this,
                        s.get(),
                        user_id[0],
                        user.toString(),
                        pass.toString());
                    }
                  }
                });
              return Unit.unit();
            }
          });
      }

    } catch (final Throwable e) {
      this.listener.onAccountLogoutFailure(
        Option.some(e), NullCheck.notNull(e.getMessage()));
    }
  }

  @Override
  public void onDeactivationError(final String message)
  {
    this.listener.onAccountLogoutFailure(Option.<Throwable>none(), message);
  }

  @Override
  public void onDeactivationSucceeded()
  {
    /**
     * Delete the books database.
     */

    try {
      this.accounts_database.accountRemoveCredentials();
      this.database.databaseDestroy();
      this.listener.onAccountLogoutSuccess();
    } catch (IOException e) {
      e.printStackTrace();
      this.listener.onAccountLogoutFailure(Option.<Throwable>some(e), e.getMessage());
    }
  }
}
