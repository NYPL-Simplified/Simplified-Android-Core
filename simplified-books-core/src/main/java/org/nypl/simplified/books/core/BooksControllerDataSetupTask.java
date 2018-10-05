package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import java.util.concurrent.Callable;

final class BooksControllerDataSetupTask implements Callable<Unit> {
  private final BookDatabaseType books_database;
  private final AccountDataSetupListenerType listener;

  BooksControllerDataSetupTask(
    final BookDatabaseType in_books_database,
    final AccountDataSetupListenerType in_listener) {

    this.books_database =
      NullCheck.notNull(in_books_database);
    this.listener =
      NullCheck.notNull(in_listener);
  }

  @Override
  public Unit call() throws Exception {
    try {
      this.books_database.databaseCreate();
      this.listener.onAccountDataSetupSuccess();
      return Unit.unit();
    } catch (final Throwable x) {
      this.listener.onAccountDataSetupFailure(Option.some(x), NullCheck.notNull(x.getMessage()));
      throw x;
    }
  }
}
