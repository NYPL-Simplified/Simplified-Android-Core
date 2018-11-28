package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import org.slf4j.Logger;

/**
 * A simple account sync listener that does nothing but log.
 */

public class AccountSyncListenerLogging implements AccountSyncListenerType {

  private final Logger logger;

  public AccountSyncListenerLogging(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public void onAccountSyncAuthenticationFailure(final String message) {
    this.logger.error("onAccountSyncAuthenticationFailure: {}", message);
  }

  @Override
  public void onAccountSyncBook(final BookID book) {
    this.logger.debug("onAccountSyncBook: {}", book.getShortID());
  }

  @Override
  public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message) {
    this.logger.error("onAccountSyncFailure: {}", message);
  }

  @Override
  public void onAccountSyncSuccess() {
    this.logger.debug("onAccountSyncSuccess");
  }

  @Override
  public void onAccountSyncBookDeleted(final BookID book) {
    this.logger.debug("onAccountSyncBookDeleted: {}", book.getShortID());
  }
}
