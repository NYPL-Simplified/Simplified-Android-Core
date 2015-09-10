package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;

/**
 * An implementation of the {@link AccountLoginListenerType} type that delegates
 * to an existing instance, and catches and logs all unchecked exceptions.
 */

final class AccountLoginListenerCatcher implements AccountLoginListenerType
{
  private final Logger                   log;
  private final AccountLoginListenerType delegate;

  AccountLoginListenerCatcher(
    final Logger in_log,
    final AccountLoginListenerType in_delegate)
  {
    this.log = NullCheck.notNull(in_log);
    this.delegate = NullCheck.notNull(in_delegate);
  }

  @Override public void onAccountLoginFailureCredentialsIncorrect()
  {
    try {
      this.delegate.onAccountLoginFailureCredentialsIncorrect();
    } catch (final Throwable e) {
      this.log.error("onAccountLoginFailureCredentialsIncorrect raised: ", e);
    }
  }

  @Override public void onAccountLoginFailureServerError(final int code)
  {
    try {
      this.delegate.onAccountLoginFailureServerError(code);
    } catch (final Throwable e) {
      this.log.error("onAccountLoginFailureServerError raised: ", e);
    }
  }

  @Override public void onAccountLoginFailureLocalError(
    final OptionType<Throwable> error,
    final String message)
  {
    try {
      this.delegate.onAccountLoginFailureLocalError(error, message);
    } catch (final Throwable e) {
      this.log.error("onAccountLoginFailureLocalError raised: ", e);
    }
  }

  @Override public void onAccountLoginSuccess(
    final AccountCredentials credentials)
  {
    try {
      this.delegate.onAccountLoginSuccess(credentials);
    } catch (final Throwable e) {
      this.log.error("onAccountLoginSuccess raised: ", e);
    }
  }

  @Override
  public void onAccountLoginFailureDeviceActivationError(final String message)
  {
    try {
      this.delegate.onAccountLoginFailureDeviceActivationError(message);
    } catch (final Throwable e) {
      this.log.error("onAccountLoginFailureDeviceActivationError raised: ", e);
    }
  }
}
