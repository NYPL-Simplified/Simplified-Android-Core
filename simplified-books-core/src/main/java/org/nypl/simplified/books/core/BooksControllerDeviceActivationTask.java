package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;

import org.nypl.drm.core.AdobeAdeptActivationReceiverType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Runnable that JUST activates the device with Adobe (used on startup, and as part of logging in)
 */

public class BooksControllerDeviceActivationTask
  implements Callable<Unit>, AdobeAdeptActivationReceiverType {

  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final AccountCredentials credentials;
  private final AccountsDatabaseType accounts_database;
  private final DeviceActivationListenerType device_activation_listener;

  private static final Logger LOG =
    LoggerFactory.getLogger(BooksControllerDeviceActivationTask.class);

  BooksControllerDeviceActivationTask(
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final AccountCredentials in_credentials,
    final AccountsDatabaseType in_accounts_database,
    final DeviceActivationListenerType in_device_activation_listener) {
    this.adobe_drm = in_adobe_drm;
    this.credentials = in_credentials;
    this.accounts_database = in_accounts_database;
    this.device_activation_listener = in_device_activation_listener;
  }

  @Override
  public void onActivation(
    final int index,
    final AdobeVendorID authority,
    final AdobeDeviceID device_id,
    final String user_name,
    final AdobeUserID user_id,
    final String expires) {

    LOG.debug("Activation [{}]: authority: {}", Integer.valueOf(index), authority);
    LOG.debug("Activation [{}]: device_id: {}", Integer.valueOf(index), device_id);
    LOG.debug("Activation [{}]: user_name: {}", Integer.valueOf(index), user_name);
    LOG.debug("Activation [{}]: user_id: {}", Integer.valueOf(index), user_id);
    LOG.debug("Activation [{}]: expires: {}", Integer.valueOf(index), expires);

    this.credentials.setAdobeUserID(Option.some(user_id));
    this.credentials.setAdobeDeviceID(Option.some(device_id));

    this.device_activation_listener.onDeviceActivationSuccess();

    try {
      this.accounts_database.accountSetCredentials(this.credentials);
    } catch (final IOException e) {
      LOG.error("could not save credentials: ", e);
    }
  }

  @Override
  public void onActivationsCount(final int count) {
    LOG.debug("Activation  count: {}", count);
  }

  @Override
  public void onActivationError(final String error) {
    LOG.debug("Failed to activate device: {}", error);
    this.device_activation_listener.onDeviceActivationFailure(error);
  }

  @Override
  public Unit call() throws Exception {
    if (this.adobe_drm.isSome()) {
      final Some<AdobeAdeptExecutorType> some = (Some<AdobeAdeptExecutorType>) this.adobe_drm;
      final AdobeAdeptExecutorType adobe_exec = some.get();
      final OptionType<AccountAdobeToken> adobe_token = this.credentials.getAdobeToken();
      final OptionType<AdobeVendorID> vendor_opt = this.credentials.getAdobeVendor();

      if (vendor_opt.isNone()) {
        this.onActivationError("No Adobe vendor ID provided");
        throw new IllegalStateException("No Adobe vendor ID provided");
      }

      CountDownLatch latch = new CountDownLatch(1);
      adobe_exec.execute(connector -> {
        final AccountAdobeToken account_token = ((Some<AccountAdobeToken>) adobe_token).get();
        final String token = account_token.toString().replace("\n", "");
        final String username = token.substring(0, token.lastIndexOf("|"));
        final String password = token.substring(token.lastIndexOf("|") + 1);

        connector.activateDevice(
          this,
          ((Some<AdobeVendorID>) vendor_opt).get(),
          username,
          password);

        new DeviceManagerPostTask(this.credentials).run();
        latch.countDown();
      });

      latch.await();
    }
    return Unit.unit();
  }
}
