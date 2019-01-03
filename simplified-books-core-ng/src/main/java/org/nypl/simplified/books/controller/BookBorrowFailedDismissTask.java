package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A task that dismisses a download.
 */

final class BookBorrowFailedDismissTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(BookBorrowFailedDismissTask.class);

  private final DownloaderType downloader;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;
  private final BookRegistryType book_registry;
  private final BookID id;
  private final BookDatabaseType book_database;

  BookBorrowFailedDismissTask(
      final DownloaderType downloader,
      final ConcurrentHashMap<BookID, DownloadType> downloads,
      final BookDatabaseType book_database,
      final BookRegistryType book_registry,
      final BookID id) {

    this.downloader =
        NullCheck.notNull(downloader, "Downloader");
    this.downloads =
        NullCheck.notNull(downloads, "Downloads");
    this.book_database =
        NullCheck.notNull(book_database, "Book database");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
    this.id =
        NullCheck.notNull(id, "ID");
  }

  @Override
  public void run() {

    LOG.debug("acknowledging download of book {}", this.id);

    final OptionType<BookStatusType> status_opt =
        this.book_registry.bookStatus(this.id);

    try {
      status_opt.mapPartial_(status -> {
        LOG.debug("status of book {} is currently {}", this.id, status);

        if (status instanceof BookStatusDownloadFailed) {
          final BookDatabaseEntryType entry = this.book_database.entry(this.id);
          this.book_registry.update(
              BookWithStatus.create(entry.book(), BookStatus.fromBook(entry.book())));
        }
      });
    } catch (final BookDatabaseException e) {
      LOG.error("Could not update book status: ", e);
    }
  }
}
