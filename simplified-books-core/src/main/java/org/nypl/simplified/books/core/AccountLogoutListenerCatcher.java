package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;

/**
 * An implementation of the {@link AccountLogoutListenerType} type that
 * delegates to an existing instance, and catches and logs all unchecked
 * exceptions.
 */

final class AccountLogoutListenerCatcher implements AccountLogoutListenerType
{
  private final Logger                    log;
  private final AccountLogoutListenerType delegate;

  AccountLogoutListenerCatcher(
    final Logger in_log,
    final AccountLogoutListenerType in_delegate)
  {
    this.log = NullCheck.notNull(in_log);
    this.delegate = NullCheck.notNull(in_delegate);
  }

  @Override public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    try {
      this.delegate.onAccountLogoutFailure(error, message);
    } catch (final Throwable e) {
      this.log.error("onAccountLogoutFailure raised: ", e);
    }
  }

  @Override public void onAccountLogoutSuccess()
  {
    try {
      this.delegate.onAccountLogoutSuccess();
    } catch (final Throwable e) {
      this.log.error("onAccountLogoutSuccess raised: ", e);
    }
  }

  @Override
  public void onAccountLogoutFailureServerError(int code) {
    try {
      this.delegate.onAccountLogoutFailureServerError(code);
    } catch (final Throwable e) {
      this.log.error("onAccountLogoutFailureServerError raised: ", e);
    }
  }
}
