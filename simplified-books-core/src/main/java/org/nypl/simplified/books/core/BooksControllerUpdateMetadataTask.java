package org.nypl.simplified.books.core;

import java.io.File;

import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

final class BooksControllerUpdateMetadataTask implements Runnable
{
  private static final Logger            LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerSyncTask.class));
  }

  private final BookID                   book_id;
  private final BookDatabaseType         books_database;
  private final OPDSAcquisitionFeedEntry entry;
  private final HTTPType                 http;

  BooksControllerUpdateMetadataTask(
    final HTTPType in_http,
    final BookDatabaseType in_book_database,
    final BookID in_id,
    final OPDSAcquisitionFeedEntry in_e)
  {
    this.http = NullCheck.notNull(in_http);
    this.books_database = NullCheck.notNull(in_book_database);
    this.book_id = NullCheck.notNull(in_id);
    this.entry = NullCheck.notNull(in_e);
  }

  @Override public void run()
  {
    try {
      final BookDatabaseEntryType e =
        this.books_database.getBookDatabaseEntry(this.book_id);

      e.create();
      e.setData(this.entry);

      final OptionType<File> cover =
        BooksController.makeCover(this.http, this.entry.getCover());

      e.setCover(cover);
    } catch (final Throwable e) {
      BooksControllerUpdateMetadataTask.LOG.error(
        "unable to update metadata: ",
        e);
    }
  }
}
