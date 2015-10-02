package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

final class BooksControllerDataLoadTask implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerDataLoadTask.class));
  }

  private final BookDatabaseType                    books_database;
  private final BooksStatusCacheType                books_status;
  private final AccountDataLoadListenerType         listener;
  private final AtomicReference<AccountCredentials> login;

  BooksControllerDataLoadTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final AccountDataLoadListenerType in_listener,
    final AtomicReference<AccountCredentials> in_login)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.listener = NullCheck.notNull(in_listener);
    this.login = NullCheck.notNull(in_login);
  }

  @Override public void run()
  {
    if (this.books_database.databaseAccountCredentialsExist()) {
      try {
        this.login.set(this.books_database.databaseAccountCredentialsGet());
      } catch (final IOException e) {
        try {
          this.listener.onAccountDataLoadFailedImmediately(e);
        } catch (final Throwable x) {
          BooksControllerDataLoadTask.LOG.error(
            "listener raised exception: ", x);
        }
      }

      this.books_database.databaseNotifyAllBookStatus(
        this.books_status,
        new ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>>()
        {
          @Override
          public void call(final Pair<BookID, BookDatabaseEntrySnapshot> p)
          {
            BooksControllerDataLoadTask.this.listener
              .onAccountDataBookLoadSucceeded(
                p.getLeft(), p.getRight());
          }
        },
        new ProcedureType<Pair<BookID, Throwable>>()
        {
          @Override public void call(final Pair<BookID, Throwable> p)
          {
            final Throwable e = p.getRight();
            final OptionType<Throwable> ex = Option.some(e);
            BooksControllerDataLoadTask.this.listener
              .onAccountDataBookLoadFailed(
                p.getLeft(), ex, e.getMessage());
          }
        });
    } else {
      try {
        this.listener.onAccountUnavailable();
      } catch (final Throwable x) {
        BooksControllerDataLoadTask.LOG.error(
          "listener raised exception: ", x);
      }
    }

    this.listener.onAccountDataBookLoadFinished();
  }
}
