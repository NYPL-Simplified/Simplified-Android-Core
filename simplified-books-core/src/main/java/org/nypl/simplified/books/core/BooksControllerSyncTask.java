package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BooksControllerSyncTask implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerSyncTask.class));
  }

  private final BookDatabaseType                 books_database;
  private final BooksControllerConfigurationType config;
  private final OPDSFeedParserType               feed_parser;
  private final HTTPType                         http;
  private final AccountSyncListenerType          listener;
  private final BooksStatusCacheType             books_status;

  BooksControllerSyncTask(
    final BooksControllerConfigurationType in_config,
    final BooksStatusCacheType in_status_cache,
    final BookDatabaseType in_books_database,
    final HTTPType in_http,
    final OPDSFeedParserType in_feed_parser,
    final DownloaderType in_downloader,
    final AccountSyncListenerType in_listener)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_status_cache);
    this.config = NullCheck.notNull(in_config);
    this.http = NullCheck.notNull(in_http);
    this.feed_parser = NullCheck.notNull(in_feed_parser);
    this.listener = NullCheck.notNull(in_listener);
    NullCheck.notNull(in_downloader);
  }

  @Override public void run()
  {
    try {
      this.sync();
      this.listener.onAccountSyncSuccess();
    } catch (final Throwable x) {
      this.listener.onAccountSyncFailure(
        Option.some(x), NullCheck.notNull(x.getMessage()));
    }
  }

  private void sync()
    throws Exception
  {
    final URI loans_uri = this.config.getCurrentLoansURI();
    final AccountCredentials c = this.books_database.credentialsGet();
    final AccountBarcode barcode = c.getUser();
    final AccountPIN pin = c.getPassword();
    final AccountSyncListenerType in_listener = this.listener;

    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());
    final HTTPResultType<InputStream> r =
      this.http.get(Option.some(auth), loans_uri, 0L);

    r.matchResult(
      new HTTPResultMatcherType<InputStream, Unit, Exception>()
      {
        @Override public Unit onHTTPError(
          final HTTPResultError<InputStream> e)
          throws Exception
        {
          final String m = NullCheck.notNull(
            String.format(
              "%s: %d: %s", loans_uri, e.getStatus(), e.getMessage()));

          switch (e.getStatus()) {
            case HttpURLConnection.HTTP_UNAUTHORIZED: {
              in_listener.onAccountSyncAuthenticationFailure("Invalid PIN");
              return Unit.unit();
            }
            default: {
              throw new IOException(m);
            }
          }
        }

        @Override public Unit onHTTPException(
          final HTTPResultException<InputStream> e)
          throws Exception
        {
          throw e.getError();
        }

        @Override public Unit onHTTPOK(
          final HTTPResultOKType<InputStream> e)
          throws Exception
        {
          try {
            BooksControllerSyncTask.this.syncFeedEntries(loans_uri, e);
            return Unit.unit();
          } finally {
            e.close();
          }
        }
      });
  }

  private void syncFeedEntries(
    final URI loans_uri,
    final HTTPResultOKType<InputStream> r_feed)
    throws Exception
  {
    final OPDSAcquisitionFeed feed =
      this.feed_parser.parse(loans_uri, r_feed.getValue());

    /**
     * Obtain the set of books that are on disk already. If any
     * of these books are not in the received feed, then they have
     * expired and should be deleted.
     */

    final List<BookDatabaseEntryType> on_disk_entries =
      this.books_database.getBookDatabaseEntries();
    final Set<BookID> existing = new HashSet<BookID>();
    for (final BookDatabaseEntryType e : on_disk_entries) {
      existing.add(e.getID());
    }

    /**
     * Handle each book in the received feed.
     */

    final Set<BookID> received = new HashSet<BookID>();
    final List<OPDSAcquisitionFeedEntry> entries = feed.getFeedEntries();
    for (final OPDSAcquisitionFeedEntry e : entries) {
      try {
        final OPDSAcquisitionFeedEntry e_nn = NullCheck.notNull(e);
        final BookID book_id = BookID.newIDFromEntry(e_nn);
        received.add(book_id);
        BooksController.syncFeedEntry(
          e_nn, this.books_database, this.books_status, this.http);
        this.listener.onAccountSyncBook(book_id);
      } catch (final Throwable x) {
        BooksControllerSyncTask.LOG.error(
          "unable to save entry: {}: ", e.getID(), x);
      }
    }

    /**
     * Now delete any book that previously existed, but is not in the
     * received set.
     */

    for (final BookID existing_id : existing) {
      try {
        if (received.contains(existing_id) == false) {
          final BookDatabaseEntryType e =
            this.books_database.getBookDatabaseEntry(existing_id);
          e.destroy();
          this.books_status.booksStatusClearFor(existing_id);
          this.listener.onAccountSyncBookDeleted(existing_id);
        }
      } catch (final Throwable x) {
        BooksControllerSyncTask.LOG.error(
          "unable to delete entry: {}: ", existing_id, x);
      }
    }
  }
}
