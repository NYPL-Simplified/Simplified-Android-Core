package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;

import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runnable that JUST activates the device with Adobe (used on startup, and as part of logging in)
 */

public class BooksControllerDeviceDeActivationTask implements Callable<Unit>,
  AdobeAdeptDeactivationReceiverType {

  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final AccountCredentials credentials;
  private final AccountsDatabaseType accounts_database;

  private static final Logger LOG =
    LoggerFactory.getLogger(BooksControllerDeviceDeActivationTask.class);

  BooksControllerDeviceDeActivationTask(
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountCredentials in_credentials,
    final AccountsDatabaseType in_accounts_database) {
    this.adobe_drm = in_adobe_drm;
    this.credentials = in_credentials;
    this.accounts_database = in_accounts_database;
  }

  @Override
  public Unit call() throws Exception {
    if (this.adobe_drm.isSome()) {
      final AdobeAdeptExecutorType adobe_exec = ((Some<AdobeAdeptExecutorType>) this.adobe_drm).get();
      final OptionType<AccountAdobeToken> adobe_token = this.credentials.getAdobeToken();
      final OptionType<AdobeVendorID> vendor_opt = this.credentials.getAdobeVendor();
      final OptionType<AdobeUserID> user_id = this.credentials.getAdobeUserID();

      if (vendor_opt.isNone()) {
        this.onDeactivationError("No Adobe vendor ID provided");
        throw new IllegalStateException("No Adobe vendor ID provided");
      }

      final CountDownLatch latch = new CountDownLatch(1);
      adobe_exec.execute(connector -> {
        try {
          if (this.credentials.getAdobeUserID().isSome()) {
            final AccountAdobeToken account_token = ((Some<AccountAdobeToken>) adobe_token).get();
            final String token = account_token.toString().replace("\n", "");
            final String username = token.substring(0, token.lastIndexOf("|"));
            final String password = token.substring(token.lastIndexOf("|") + 1);

            connector.deactivateDevice(
              this,
              ((Some<AdobeVendorID>) vendor_opt).get(),
              ((Some<AdobeUserID>) user_id).get(),
              username,
              password);
          } else {
            this.onDeactivationSucceeded();
          }
        } finally {
          latch.countDown();
        }
      });

      latch.await();
    }

    return Unit.unit();
  }

  @Override
  public void onDeactivationError(final String message) {
    LOG.debug("Failed to deactivate device: {}", message);
  }

  @Override
  public void onDeactivationSucceeded() {

    /*
     * Device deactivation succeeded.
     */

    this.credentials.setAdobeUserID(Option.<AdobeUserID>none());
    this.credentials.setAdobeDeviceID(Option.<AdobeDeviceID>none());

    try {
      this.accounts_database.accountSetCredentials(this.credentials);
    } catch (final IOException e) {
      LOG.error("could not save credentials: ", e);
    }
  }
}
