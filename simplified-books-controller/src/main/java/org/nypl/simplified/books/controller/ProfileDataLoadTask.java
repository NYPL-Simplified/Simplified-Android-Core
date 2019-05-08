package org.nypl.simplified.books.controller;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.books.api.Book;
import org.nypl.simplified.books.api.BookID;
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.api.BookDatabaseException;
import org.nypl.simplified.books.book_database.api.BookDatabaseType;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.profiles.api.ProfileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.SortedMap;

final class ProfileDataLoadTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileDataLoadTask.class);

  private final ProfileType profile;
  private final BookRegistryType book_registry;

  ProfileDataLoadTask(
      final ProfileType profile,
      final BookRegistryType book_registry) {

    this.profile =
        NullCheck.notNull(profile, "Profile");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
  }

  @Override
  public void run() {
    LOG.debug("load: profile {}", this.profile.displayName());

    final SortedMap<AccountID, AccountType> accounts = this.profile.accounts();
    for (final AccountType account : accounts.values()) {
      LOG.debug("load: profile {} / account {}", this.profile.displayName(), account.id());
      final BookDatabaseType books = account.bookDatabase();
      final Collection<BookID> book_ids = books.books();
      LOG.debug("load: updating {} books", book_ids.size());
      for (final BookID book_id : book_ids) {
        try {
          final BookDatabaseEntryType entry = books.entry(book_id);
          final Book book = entry.getBook();
          final BookStatusType status = BookStatus.fromBook(book);
          this.book_registry.update(BookWithStatus.create(book, status));
        } catch (BookDatabaseException e) {
          LOG.error("load: could not load book {}: ", book_id.value(), e);
        }
      }
    }
  }
}
