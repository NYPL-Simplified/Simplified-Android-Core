package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

final class BookRevokeFailedDismissTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BookRevokeFailedDismissTask.class);

  private final BookDatabaseType book_database;
  private final BookRegistryType book_registry;
  private final BookID book_id;

  BookRevokeFailedDismissTask(
    final BookDatabaseType book_database,
    final BookRegistryType book_registry,
    final BookID book_id) {

    this.book_database =
      NullCheck.notNull(book_database, "book_database");
    this.book_registry =
      NullCheck.notNull(book_registry, "book_registry");
    this.book_id =
      NullCheck.notNull(book_id, "book_id");
  }

  @Override
  public Unit call() throws Exception {
    try {
      LOG.debug("[{}] revoke failure dismiss", this.book_id.brief());

      final OptionType<BookStatusType> status_opt =
        this.book_registry.bookStatus(this.book_id);

      status_opt.mapPartial_(status -> {
        LOG.debug("[{}] status of book is currently {}", this.book_id.brief(), status);

        if (status instanceof BookStatusRevokeFailed) {
          final BookDatabaseEntryType entry = this.book_database.entry(this.book_id);
          final Book book = entry.getBook();
          final BookStatusType new_status = BookStatus.fromBook(book);
          this.book_registry.update(BookWithStatus.create(book, new_status));
          LOG.debug("[{}] status of book is now {}", this.book_id.brief(), new_status);
        }
      });
    } finally {
      LOG.debug("[{}] revoke failure dismiss finished", this.book_id.brief());
    }
    return Unit.unit();
  }
}
