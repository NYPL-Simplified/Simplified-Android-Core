package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.files.DirectoryUtilities;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

final class BooksControllerLogoutTask implements Runnable
{
  private final File                                              base;
  private final BooksControllerConfiguration                      config;
  private final AccountLogoutListenerType                         listener;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;

  public BooksControllerLogoutTask(
    final BooksControllerConfiguration in_config,
    final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login,
    final AccountLogoutListenerType in_listener)
  {
    this.config = NullCheck.notNull(in_config);
    this.listener = NullCheck.notNull(in_listener);
    this.login = NullCheck.notNull(in_login);
    this.base = new File(this.config.getDirectory(), "data");
  }

  @Override public void run()
  {
    try {
      this.login.set(null);

      if (this.base.isDirectory()) {
        DirectoryUtilities.directoryDelete(this.base);
      } else {
        throw new IllegalStateException("Not logged in");
      }

      this.listener.onAccountLogoutSuccess();
    } catch (final Throwable e) {
      this.listener.onAccountLogoutFailure(
        Option.some(e), NullCheck.notNull(e.getMessage()));
    }
  }
}
