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

      final OptionType<AccountAdobeToken> adobe_token = this.credentials.getAdobeToken();
      final OptionType<AdobeVendorID> vendor_opt = this.credentials.getAdobeVendor();
      final OptionType<AdobeUserID> user_id = this.credentials.getAdobeUserID();

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
                  
                  if (BooksControllerDeviceDeActivationTask.this.credentials.getAdobeUserID().isSome()) {

                    final String token = ((Some<AccountAdobeToken>) adobe_token).get().toString().replace("\n", "");
                    final String username = token.substring(0, token.lastIndexOf("|"));
                    final String password = token.substring(token.lastIndexOf("|") + 1);

                    c.deactivateDevice(
                      BooksControllerDeviceDeActivationTask.this,
                      ((Some<AdobeVendorID>) vendor_opt).get(),
                      ((Some<AdobeUserID>) user_id).get(),
                      username,
                      password);

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
