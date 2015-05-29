package org.nypl.simplified.books.core;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.downloader.core.DownloaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;

final class BooksControllerDataLoadTask implements Runnable
{
  private static final Logger                                     LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerDataLoadTask.class));
  }

  private final BookDatabaseType                                  books_database;
  private final BooksStatusCacheType                              books_status;
  private final DownloaderType                                    downloader;
  private final AccountDataLoadListenerType                       listener;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;

  BooksControllerDataLoadTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final DownloaderType in_downloader,
    final AccountDataLoadListenerType in_listener,
    final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.downloader = NullCheck.notNull(in_downloader);
    this.listener = NullCheck.notNull(in_listener);
    this.login = NullCheck.notNull(in_login);
  }

  private void loadBooks()
  {
    final List<BookDatabaseEntryType> book_list =
      this.books_database.getBookDatabaseEntries();
    for (final BookDatabaseEntryReadableType book_dir : book_list) {
      final BookID id = book_dir.getID();
      try {
        final BookSnapshot snap = book_dir.getSnapshot();
        final BookStatusLoanedType status =
          BookStatus.fromBookSnapshot(this.downloader, id, snap);

        this.books_status.booksStatusUpdate(status);
        this.books_status.booksSnapshotUpdate(id, snap);
        this.listener.onAccountDataBookLoadSucceeded(id, snap);
      } catch (final Throwable e) {
        this.listener.onAccountDataBookLoadFailed(
          id,
          Option.some(e),
          NullCheck.notNull(e.getMessage()));
      }
    }
  }

  @Override public void run()
  {
    if (this.books_database.credentialsExist()) {
      try {
        this.login.set(this.books_database.credentialsGet());
      } catch (final IOException e) {
        try {
          this.listener.onAccountDataLoadFailedImmediately(e);
        } catch (final Throwable x) {
          BooksControllerDataLoadTask.LOG.error(
            "listener raised exception: ",
            x);
        }
      }

      this.loadBooks();
    } else {
      try {
        this.listener.onAccountUnavailable();
      } catch (final Throwable x) {
        BooksControllerDataLoadTask.LOG.error(
          "listener raised exception: ",
          x);
      }
    }

    this.listener.onAccountDataBookLoadFinished();
  }
}
