package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

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

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

@SuppressWarnings({ "boxing", "synthetic-access" }) final class BooksControllerSyncTask implements
  Runnable
{
  private static final Logger                LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerSyncTask.class));
  }

  private final BookDatabaseType             books_database;
  private final BooksControllerConfiguration config;
  private final OPDSFeedParserType           feed_parser;
  private final HTTPType                     http;
  private final AccountSyncListenerType      listener;
  private final BooksStatusCacheType         status_cache;

  BooksControllerSyncTask(
    final BooksControllerConfiguration in_config,
    final BooksStatusCacheType in_status_cache,
    final BookDatabaseType in_books_database,
    final HTTPType in_http,
    final OPDSFeedParserType in_feed_parser,
    final DownloaderType in_downloader,
    final AccountSyncListenerType in_listener)
  {
    this.books_database = NullCheck.notNull(in_books_database);
    this.status_cache = NullCheck.notNull(in_status_cache);
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
        Option.some(x),
        NullCheck.notNull(x.getMessage()));
    }
  }

  private void sync()
    throws Exception
  {
    final Pair<AccountBarcode, AccountPIN> pair =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = pair.getLeft();
    final AccountPIN pin = pair.getRight();

    final AccountSyncListenerType in_listener = this.listener;
    final URI loans_uri = this.config.getLoansURI();

    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());
    final HTTPResultType<InputStream> r =
      this.http.get(Option.some(auth), loans_uri, 0);

    r.matchResult(new HTTPResultMatcherType<InputStream, Unit, Exception>() {
      @Override public Unit onHTTPError(
        final HTTPResultError<InputStream> e)
        throws Exception
      {
        final String m =
          NullCheck.notNull(String.format(
            "%s: %d: %s",
            loans_uri,
            e.getStatus(),
            e.getMessage()));

        switch (e.getStatus()) {
          case HttpURLConnection.HTTP_UNAUTHORIZED:
          {
            in_listener.onAccountSyncAuthenticationFailure("Invalid PIN");
            return Unit.unit();
          }
          default:
          {
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

    final List<OPDSAcquisitionFeedEntry> entries = feed.getFeedEntries();
    for (final OPDSAcquisitionFeedEntry e : entries) {
      try {
        this.syncFeedEntry(NullCheck.notNull(e));
      } catch (final Throwable x) {
        BooksControllerSyncTask.LOG.error(
          "unable to save entry: {}: ",
          e.getID(),
          x);
      }
    }
  }

  private void syncFeedEntry(
    final OPDSAcquisitionFeedEntry e)
    throws Exception
  {
    final BookID book_id = BookID.newIDFromEntry(e);

    final BookDatabaseEntryType book_dir =
      this.books_database.getBookDatabaseEntry(book_id);

    book_dir.create();
    book_dir.setData(e);

    final OptionType<File> cover =
      BooksController.makeCover(this.http, e.getCover());
    book_dir.setCover(cover);

    final BookSnapshot snap = book_dir.getSnapshot();
    final BookStatusType status = BookStatus.fromSnapshot(book_id, snap);

    this.status_cache.booksStatusUpdateIfMoreImportant(status);
    this.status_cache.booksSnapshotUpdate(book_id, snap);
    this.listener.onAccountSyncBook(book_id);
  }
}
