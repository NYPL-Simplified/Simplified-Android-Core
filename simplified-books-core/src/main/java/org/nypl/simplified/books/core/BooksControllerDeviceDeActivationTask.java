package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;

import org.nypl.drm.core.AdobeAdeptActivationReceiverType;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Runnable that JUST activates the device with Adobe (used on startup, and as part of logging in)
 */

public class BooksControllerDeviceDeActivationTask implements Runnable,
  AdobeAdeptDeactivationReceiverType {

  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final AccountCredentials credentials;
  private final BookDatabaseType book_database;
  private final AccountsDatabaseType accounts_database;

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(BooksControllerDeviceDeActivationTask.class);
  }

  BooksControllerDeviceDeActivationTask(
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountCredentials in_credentials,
    final AccountsDatabaseType in_accounts_database,
    final BookDatabaseType in_book_database) {
    this.adobe_drm = in_adobe_drm;
    this.credentials = in_credentials;
    this.book_database = in_book_database;
    this.accounts_database = in_accounts_database;
  }

  @Override
  public void run() {
    if (this.adobe_drm.isSome()) {
      final Some<AdobeAdeptExecutorType> some =
        (Some<AdobeAdeptExecutorType>) this.adobe_drm;
      final AdobeAdeptExecutorType adobe_exec = some.get();

      final AccountBarcode user = this.credentials.getBarcode();
      final AccountPIN pass = this.credentials.getPin();
      final OptionType<AccountAdobeToken> adobe_token = this.credentials.getAdobeToken();
      final OptionType<AdobeVendorID> vendor_opt = this.credentials.getAdobeVendor();
//      final OptionType<AdobeUserID> user_id = this.credentials.getAdobeUserID();

      vendor_opt.accept(
        new OptionVisitorType<AdobeVendorID, Unit>() {
          @Override
          public Unit none(final None<AdobeVendorID> n) {
            BooksControllerDeviceDeActivationTask.this.onDeactivationError(
              "No Adobe vendor ID provided");
            return Unit.unit();
          }


          @Override
          public Unit some(final Some<AdobeVendorID> s) {
            adobe_exec.execute(
              new AdobeAdeptProcedureType() {
                @Override
                public void executeWith(final AdobeAdeptConnectorType c) {


                  if (BooksControllerDeviceDeActivationTask.this.credentials.getAdobeUserID().isNone()) {

                    c.getDeviceActivations(new AdobeAdeptActivationReceiverType() {
                      @Override
                      public void onActivationsCount(final int count) {
                        if (count == 0) {
                          BooksControllerDeviceDeActivationTask.this.onDeactivationError("No devices were activated.");
                        }
                      }

                      @Override
                      public void onActivation(final int index,
                                               final AdobeVendorID authority,
                                               final AdobeDeviceID device_id,
                                               final String user_name,
                                               final AdobeUserID in_user_id,
                                               final String expires) {


                        final AdobeDeviceID adobe_device_id = device_id;


                        BooksControllerDeviceDeActivationTask.this.credentials.setAdobeUserID(Option.some(in_user_id));
                        BooksControllerDeviceDeActivationTask.this.credentials.setAdobeDeviceID(Option.some(adobe_device_id));

                        try {

                          BooksControllerDeviceDeActivationTask.this.accounts_database.accountSetCredentials(BooksControllerDeviceDeActivationTask.this.credentials);
                        } catch (final IOException e) {
                          BooksControllerDeviceDeActivationTask.LOG.error("could not save credentials: ", e);
                        }


                      }

                      @Override
                      public void onActivationError(final String message) {
                        BooksControllerDeviceDeActivationTask.this.onDeactivationError("Activation error when getting existing activations. This should never happen.");
                      }
                    });

                  }


                  if (BooksControllerDeviceDeActivationTask.this.credentials.getAdobeUserID().isSome()) {

                    if (adobe_token.isNone()) {
                      c.deactivateDevice(
                        BooksControllerDeviceDeActivationTask.this,
                        s.get(),
                        ((Some<AdobeUserID>) BooksControllerDeviceDeActivationTask.this.credentials.getAdobeUserID()).get(),
                        user.toString(),
                        pass.toString());
                    } else {
                      c.deactivateDevice(
                        BooksControllerDeviceDeActivationTask.this,
                        s.get(),
                        ((Some<AdobeUserID>) BooksControllerDeviceDeActivationTask.this.credentials.getAdobeUserID()).get(),
                        ((Some<AccountAdobeToken>) adobe_token).get().toString(),
                        "");
                    }

                  } else {
                    BooksControllerDeviceDeActivationTask.this.onDeactivationSucceeded();
                  }

                }
              });
            return Unit.unit();
          }

        });
    }
  }


  @Override
  public void onDeactivationError(final String message) {
    BooksControllerDeviceDeActivationTask.LOG.debug("Failed to deactivate device: {}", message);
  }

  @Override
  public void onDeactivationSucceeded() {
    /**
     * Device deactivation succeeded.
     */

    BooksControllerDeviceDeActivationTask.this.credentials.setAdobeUserID(Option.<AdobeUserID>none());
    BooksControllerDeviceDeActivationTask.this.credentials.setAdobeDeviceID(Option.<AdobeDeviceID>none());

    try {
      this.accounts_database.accountSetCredentials(this.credentials);
    } catch (final IOException e) {
      BooksControllerDeviceDeActivationTask.LOG.error("could not save credentials: ", e);
    }

  }

}
