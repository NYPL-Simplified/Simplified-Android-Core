package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> The logic for borrowing and/or fulfilling a book. </p>
 */

@SuppressWarnings("synthetic-access") final class BooksControllerBorrowTask
  implements Runnable,
  DownloadListenerType,
  FeedLoaderListenerType,
  FeedMatcherType<Unit, Exception>,
  FeedEntryMatcherType<Unit, Exception>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(
        BooksControllerBorrowTask.class));
  }

  private final OPDSAcquisition           acq;
  private final BookID                    book_id;
  private final BookDatabaseType          books_database;
  private final BooksStatusCacheType      books_status;
  private final DownloaderType            downloader;
  private final Map<BookID, DownloadType> downloads;
  private final FeedLoaderType            feed_loader;
  private final HTTPType                  http;
  private final BookBorrowListenerType    listener;
  private final OPDSAcquisitionFeedEntry  feed_entry;

  BooksControllerBorrowTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final DownloaderType in_downloader,
    final HTTPType in_http,
    final ConcurrentHashMap<BookID, DownloadType> in_downloads,
    final BookID in_book_id,
    final OPDSAcquisition in_acq,
    final OPDSAcquisitionFeedEntry in_feed_entry,
    final BookBorrowListenerType in_listener,
    final FeedLoaderType in_feed_loader)
  {
    this.downloader = NullCheck.notNull(in_downloader);
    this.downloads = NullCheck.notNull(in_downloads);
    this.http = NullCheck.notNull(in_http);
    this.book_id = NullCheck.notNull(in_book_id);
    this.acq = NullCheck.notNull(in_acq);
    this.feed_entry = NullCheck.notNull(in_feed_entry);
    this.listener = NullCheck.notNull(in_listener);
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.feed_loader = NullCheck.notNull(in_feed_loader);
  }

  private void downloadFailed(
    final OptionType<Throwable> exception)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadFailed status =
      new BookStatusDownloadFailed(this.book_id, exception, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadCancelled(
    final DownloadType d)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusLoaned status = new BookStatusLoaned(this.book_id, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadCompleted(
    final DownloadType d,
    final File file)
    throws IOException
  {
    BooksControllerBorrowTask.LOG.debug(
      "download {} completed for {}", d, file);

    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);

    e.copyInBookFromSameFilesystem(file);
    file.delete();

    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloaded status =
      new BookStatusDownloaded(this.book_id, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadDataReceived(
    final DownloadType d,
    final long running_total,
    final long expected_total)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(
        this.book_id, running_total, expected_total, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadFailed(
    final DownloadType d,
    final int status_code,
    final long running_total,
    final OptionType<Throwable> exception)
  {
    this.downloadFailed(exception);
  }

  @Override public void onDownloadStarted(
    final DownloadType d,
    final long expected_total)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, 0L, expected_total, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public Unit onFeedEntryCorrupt(
    final FeedEntryCorrupt e)
    throws IOException
  {
    BooksControllerBorrowTask.LOG.error(
      "unexpectedly received corrupt feed entry");
    throw new IOException(e.getError());
  }

  @Override public Unit onFeedEntryOPDS(
    final FeedEntryOPDS e)
    throws Exception
  {
    BooksControllerBorrowTask.LOG.debug("received OPDS feed entry");

    final OPDSAcquisitionFeedEntry ee = e.getFeedEntry();
    final OPDSAvailabilityType availability = ee.getAvailability();

    BooksControllerBorrowTask.LOG.debug(
      "book availability is {}", availability);

    final BookID b_id = this.book_id;
    final BooksStatusCacheType stat = this.books_status;
    availability.matchAvailability(
      new OPDSAvailabilityMatcherType<Unit, Exception>()
      {
        @Override public Unit onHeld(
          final OPDSAvailabilityHeld a)
        {
          final BookStatusHeld status =
            new BookStatusHeld(b_id, a.getPosition());
          stat.booksStatusUpdate(status);
          return Unit.unit();
        }

        @Override public Unit onHoldable(
          final OPDSAvailabilityHoldable a)
        {
          final BookStatusHoldable status = new BookStatusHoldable(b_id);
          stat.booksStatusUpdate(status);
          return Unit.unit();
        }

        @Override public Unit onLoanable(
          final OPDSAvailabilityLoanable a)
        {

          return Unit.unit();
        }

        @Override public Unit onLoaned(
          final OPDSAvailabilityLoaned a)
          throws Exception
        {
          final BookStatusLoaned status =
            new BookStatusLoaned(b_id, a.getEndDate());
          stat.booksStatusUpdate(status);

          BooksControllerBorrowTask.this.downloads.put(
            b_id, BooksControllerBorrowTask.this.runAcquisitionFulfill(ee));

          return Unit.unit();
        }

        @Override public Unit onOpenAccess(
          final OPDSAvailabilityOpenAccess a)
        {
          return Unit.unit();
        }
      });

    return Unit.unit();
  }

  @Override public void onFeedLoadFailure(
    final URI u,
    final Throwable x)
  {
    BooksControllerBorrowTask.LOG.debug("failed to load feed: {}: ", x);
    this.downloadFailed(Option.some(x));
    this.listener.onBookBorrowFailure(this.book_id, Option.some(x));
  }

  @Override public void onFeedLoadSuccess(
    final URI u,
    final FeedType f)
  {
    try {
      BooksControllerBorrowTask.LOG.debug("loaded feed from {}", u);
      f.matchFeed(this);
    } catch (final Exception e) {
      this.listener.onBookBorrowFailure(
        this.book_id, Option.some((Throwable) e));
    }
  }

  @Override public Unit onFeedWithGroups(
    final FeedWithGroups f)
    throws Exception
  {
    final FeedGroup g = NullCheck.notNull(f.get(0));
    final FeedEntryType e = NullCheck.notNull(g.getGroupEntries().get(0));
    return e.matchFeedEntry(this);
  }

  @Override public Unit onFeedWithoutGroups(
    final FeedWithoutGroups f)
    throws Exception
  {
    final FeedEntryType e = NullCheck.notNull(f.get(0));
    return e.matchFeedEntry(this);
  }

  @Override public void run()
  {
    try {
      BooksControllerBorrowTask.LOG.debug("creating feed entry");

      BooksController.syncFeedEntry(
        this.feed_entry, this.books_database, this.books_status, this.http);

      switch (this.acq.getType()) {
        case ACQUISITION_BORROW: {
          this.runAcquisitionBorrow();
          break;
        }
        case ACQUISITION_GENERIC:
        case ACQUISITION_OPEN_ACCESS: {
          this.runAcquisitionFulfill(this.feed_entry);
          break;
        }
        case ACQUISITION_BUY:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE: {
          throw new UnimplementedCodeException();
        }
      }

    } catch (final Throwable e) {
      BooksControllerBorrowTask.LOG.error("error: ", e);
      this.listener.onBookBorrowFailure(this.book_id, Option.some(e));
    }
  }

  /**
   * Hit a "borrow" link, read the resulting feed, download the book if it is
   * available.
   */

  private void runAcquisitionBorrow()
  {
    BooksControllerBorrowTask.LOG.debug(
      "fetching item feed: {}", this.acq.getURI());
    this.feed_loader.fromURIRefreshing(this.acq.getURI(), this);
  }

  private DownloadType runDownload(
    final OPDSAcquisition a)
    throws Exception
  {
    BooksControllerBorrowTask.LOG.debug(
      "book {}: starting download", this.book_id);

    final Pair<AccountBarcode, AccountPIN> p =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = p.getLeft();
    final AccountPIN pin = p.getRight();
    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());

    return this.downloader.download(a.getURI(), Option.some(auth), this);
  }

  /**
   * Fulfill a book by hitting the generic or open access links.
   */

  private DownloadType runAcquisitionFulfill(
    final OPDSAcquisitionFeedEntry ee)
    throws Exception
  {
    for (final OPDSAcquisition ea : ee.getAcquisitions()) {
      switch (ea.getType()) {
        case ACQUISITION_GENERIC:
        case ACQUISITION_OPEN_ACCESS: {
          return this.runDownload(ea);
        }
        case ACQUISITION_BORROW:
        case ACQUISITION_BUY:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE: {
          break;
        }
      }
    }

    throw new IOException("No usable acquisition link");
  }

}
