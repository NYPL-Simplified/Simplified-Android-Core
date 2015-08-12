package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptFulfillmentListenerType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.nypl.drm.core.DRMUnsupportedException;
import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.FileUtilities;
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
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>The logic for borrowing and/or fulfilling a book.</p>
 */

final class BooksControllerBorrowTask implements Runnable,
  DownloadListenerType,
  FeedLoaderListenerType,
  FeedMatcherType<Unit, Exception>,
  FeedEntryMatcherType<Unit, Exception>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerBorrowTask.class));
  }

  private final OPDSAcquisition                    acq;
  private final BookID                             book_id;
  private final BookDatabaseType                   books_database;
  private final BooksStatusCacheType               books_status;
  private final DownloaderType                     downloader;
  private final Map<BookID, DownloadType>          downloads;
  private final FeedLoaderType                     feed_loader;
  private final HTTPType                           http;
  private final BookBorrowListenerType             listener;
  private final OPDSAcquisitionFeedEntry           feed_entry;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;

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
    final FeedLoaderType in_feed_loader,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm)
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
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
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
    this.downloadCancelled();
  }

  private void downloadCancelled()
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

    /**
     * If the downloaded file is an ACSM fulfillment token, then the book
     * must be downloaded using the Adobe DRM interface.
     */

    if ("application/vnd.adobe.adept+xml".equals(d.getContentType())) {
      this.runFulfillACSM(file);
    } else {

      /**
       * Otherwise, assume it's an EPUB and keep it.
       */

      final OptionType<ByteBuffer> none = Option.none();
      this.saveEPUBAndRights(file, none);
    }
  }

  /**
   * Save an EPUB file for the current book, with optional rights information.
   *
   * @param file   The EPUB
   * @param rights The rights information
   *
   * @throws IOException On I/O errors
   */

  private void saveEPUBAndRights(
    final File file,
    final OptionType<ByteBuffer> rights)
    throws IOException
  {
    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);
    e.copyInBook(file);
    e.setAdobeRightsInformation(rights);

    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloaded status =
      new BookStatusDownloaded(this.book_id, none);
    this.books_status.booksStatusUpdate(status);
  }

  /**
   * Fulfill the given ACSM file, if Adobe DRM is supported. Otherwise, fail.
   *
   * @param file The ACSM file
   *
   * @throws IOException On I/O errors
   */

  private void runFulfillACSM(final File file)
    throws IOException
  {
    if (this.adobe_drm.isSome()) {
      final AdobeAdeptExecutorType adobe =
        ((Some<AdobeAdeptExecutorType>) this.adobe_drm).get();
      this.runFulfillACSMWithConnector(adobe, file);
    } else {
      final DRMUnsupportedException ex =
        new DRMUnsupportedException("DRM support is not available");
      this.downloadFailed(Option.some((Throwable) ex));
    }
  }

  private void runFulfillACSMWithConnector(
    final AdobeAdeptExecutorType adobe,
    final File file)
    throws IOException
  {
    final byte[] acsm = FileUtilities.fileReadBytes(file);
    adobe.execute(
      new AdobeAdeptProcedureType()
      {
        @Override public void executeWith(final AdobeAdeptConnectorType c)
        {
          c.fulfillACSM(new AdobeFulfillmentListener(), acsm);
        }
      });
  }

  @Override public void onDownloadDataReceived(
    final DownloadType d,
    final long running_total,
    final long expected_total)
  {
    this.downloadDataReceived(running_total, expected_total);
  }

  private void downloadDataReceived(
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
    this.downloadDataReceived(0L, expected_total);
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
          final BookStatusRequestingDownload status =
            new BookStatusRequestingDownload(b_id, a.getEndDate());
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

      /**
       * First, create the on-disk database entry for the book.
       */

      BooksController.syncFeedEntry(
        this.feed_entry, this.books_database, this.books_status, this.http);
      this.books_status.booksStatusUpdate(
        new BookStatusRequestingLoan(this.book_id));

      /**
       * Then, run the appropriate acquisition type for the book.
       */

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
    throws IOException
  {
    /**
     * Borrowing requires authentication.
     */

    final Pair<AccountBarcode, AccountPIN> p =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = p.getLeft();
    final AccountPIN pin = p.getRight();
    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());

    /**
     * Grab the feed for the borrow link.
     */

    BooksControllerBorrowTask.LOG.debug(
      "fetching item feed: {}", this.acq.getURI());

    this.feed_loader.fromURIRefreshing(
      this.acq.getURI(), Option.some(auth), this);
  }

  private DownloadType runDownload(
    final OPDSAcquisition a)
    throws Exception
  {
    /**
     * Downloading requires authentication.
     */

    final Pair<AccountBarcode, AccountPIN> p =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = p.getLeft();
    final AccountPIN pin = p.getRight();
    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());

    BooksControllerBorrowTask.LOG.debug(
      "book {}: starting download", this.book_id);

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

  /**
   * The listener passed to the Adobe library in order to perform fulfillment of
   * tokens delivered in ACSM files.
   */

  private final class AdobeFulfillmentListener
    implements AdobeAdeptFulfillmentListenerType
  {
    AdobeFulfillmentListener()
    {

    }

    @Override public void onFulfillmentFailure(final String error)
    {
      final OptionType<Throwable> none = Option.none();
      BooksControllerBorrowTask.this.downloadFailed(none);
    }

    @Override public void onFulfillmentSuccess(
      final File file,
      final byte[] rights)
    {
      try {
        final OptionType<ByteBuffer> rights_data = Option.some(
          ByteBuffer.wrap(rights));
        BooksControllerBorrowTask.this.saveEPUBAndRights(file, rights_data);
      } catch (final IOException x) {
        BooksControllerBorrowTask.this.downloadFailed(
          Option.some((Throwable) x));
      }
    }

    @Override public void onFulfillmentProgress(final double progress)
    {
      /**
       * The Adobe library won't give exact numbers when it comes to bytes,
       * but the app doesn't actually need to display those anyway. We therefore
       * assume that an ebook is 10000 bytes long, and calculate byte values
       * as if this were true!
       */

      BooksControllerBorrowTask.this.downloadDataReceived(
        (long) (10000L * progress), 10000L);
    }

    @Override public void onFulfillmentCancelled()
    {
      BooksControllerBorrowTask.this.downloadCancelled();
    }
  }
}
