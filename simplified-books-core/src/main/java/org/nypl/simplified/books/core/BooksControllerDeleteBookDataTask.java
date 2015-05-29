package org.nypl.simplified.books.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jnull.NullCheck;

final class BooksControllerDeleteBookDataTask implements Runnable
{
  private static final Logger        LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerDeleteBookDataTask.class));
  }

  private final BookDatabaseType     book_database;
  private final BookID               book_id;
  private final BooksStatusCacheType books_status;

  public BooksControllerDeleteBookDataTask(
    final BooksStatusCacheType in_books_status,
    final BookDatabaseType in_book_database,
    final BookID in_book_id)
  {
    this.books_status = NullCheck.notNull(in_books_status);
    this.book_database = NullCheck.notNull(in_book_database);
    this.book_id = NullCheck.notNull(in_book_id);
  }

  @Override public void run()
  {
    try {
      final BookDatabaseEntryType e =
        this.book_database.getBookDatabaseEntry(this.book_id);
      e.destroyBookData();

      this.books_status.booksStatusUpdate(new BookStatusLoaned(this.book_id));
    } catch (final Throwable e) {
      BooksControllerDeleteBookDataTask.LOG.error(
        "could not destroy book data for {}: ",
        this.book_id,
        e);
    }
  }
}
