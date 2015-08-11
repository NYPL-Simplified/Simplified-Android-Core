package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jnull.NullCheck;

final class BooksControllerDataSetupTask implements Runnable
{
  private final BookDatabaseType             books_database;
  private final AccountDataSetupListenerType listener;

  public BooksControllerDataSetupTask(
    final BookDatabaseType in_books_database,
    final AccountDataSetupListenerType in_listener)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.listener = NullCheck.notNull(in_listener);
  }

  @Override public void run()
  {
    try {
      this.books_database.create();
      this.listener.onAccountDataSetupSuccess();
    } catch (final Throwable x) {
      this.listener.onAccountDataSetupFailure(
        Option.some(x), NullCheck.notNull(x.getMessage()));
    }
  }
}
