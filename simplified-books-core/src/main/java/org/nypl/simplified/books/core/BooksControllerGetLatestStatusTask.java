package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * A task that simply loads the on-disk book status from the database and
 * broadcasts it.
 */

final class BooksControllerGetLatestStatusTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerSyncTask.class);

  private final BookID book_id;
  private final BookDatabaseType books_database;
  private final BooksStatusCacheType book_status;

  BooksControllerGetLatestStatusTask(
    final BookDatabaseType in_book_database,
    final BooksStatusCacheType in_book_status,
    final BookID in_id) {
    this.books_database = NullCheck.notNull(in_book_database);
    this.book_id = NullCheck.notNull(in_id);
    this.book_status = NullCheck.notNull(in_book_status);
  }

  @Override
  public Unit call() throws Exception {
    try {
      final BookDatabaseEntryReadableType e =
        this.books_database.databaseOpenExistingEntry(this.book_id);
      final BookDatabaseEntrySnapshot snap = e.entryGetSnapshot();
      this.book_status.booksStatusUpdate(BookStatus.Companion.fromSnapshot(this.book_id, snap));
      return Unit.unit();
    } catch (final IOException e) {
      LOG.error("[{}]: unable to fetch status: ", this.book_id.getShortID(), e);
      this.book_status.booksStatusClearFor(this.book_id);
      throw e;
    }
  }
}
