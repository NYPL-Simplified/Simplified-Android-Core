package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.books.api.Book;
import org.nypl.simplified.books.api.BookID;
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.api.BookDatabaseException;
import org.nypl.simplified.books.book_database.api.BookDatabaseType;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

final class BookDeleteTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BookDeleteTask.class);

  private final AccountType account;
  private final BookRegistryType book_registry;
  private final BookID book_id;

  BookDeleteTask(
      final AccountType account,
      final BookRegistryType book_registry,
      final BookID book_id) {

    this.account =
        NullCheck.notNull(account, "Account");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
    this.book_id =
        NullCheck.notNull(book_id, "Book id");
  }

  @Override
  public Unit call() throws Exception {
    execute();
    return Unit.unit();
  }

  private void execute() throws BookDatabaseException {
    LOG.debug("[{}] deleting book", this.book_id.brief());

    final BookDatabaseType book_database = this.account.bookDatabase();
    final BookDatabaseEntryType entry = book_database.entry(this.book_id);
    entry.delete();
    final Book book = entry.getBook();
    this.book_registry.clearFor(book_id);
  }
}
