package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeAdeptACSMException;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.bundled_content.BundledContentResolverType;
import org.nypl.simplified.books.bundled_content.BundledURIs;
import org.nypl.simplified.books.exceptions.BookBorrowExceptionBadBorrowFeed;
import org.nypl.simplified.books.exceptions.BookBorrowExceptionFetchingBorrowFeedFailed;
import org.nypl.simplified.books.exceptions.BookBorrowExceptionLoanLimitReached;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed;
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress;
import org.nypl.simplified.books.book_registry.BookStatusHeld;
import org.nypl.simplified.books.book_registry.BookStatusHeldReady;
import org.nypl.simplified.books.book_registry.BookStatusHoldable;
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload;
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan;
import org.nypl.simplified.books.logging.LogUtilities;
import org.nypl.simplified.books.feeds.FeedEntryCorrupt;
import org.nypl.simplified.books.feeds.FeedEntryMatcherType;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.books.feeds.FeedEntryType;
import org.nypl.simplified.books.feeds.FeedGroup;
import org.nypl.simplified.books.feeds.FeedHTTPTransportException;
import org.nypl.simplified.books.feeds.FeedLoaderAuthenticationListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.feeds.FeedMatcherType;
import org.nypl.simplified.books.feeds.FeedType;
import org.nypl.simplified.books.feeds.FeedWithGroups;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A book borrowing task.
 */

final class BookBorrowTask implements Callable<Unit> {

  private static final String ACSM_CONTENT_TYPE = "application/vnd.adobe.adept+xml";

  private static final Logger LOG = LoggerFactory.getLogger(BookBorrowTask.class);

  private final FeedLoaderType feed_loader;
  private final BundledContentResolverType bundled_content;
  private final BookRegistryType book_registry;
  private final BookID book_id;
  private final AccountType account;
  private final OPDSAcquisition acquisition;
  private final OPDSAcquisitionFeedEntry entry;
  private final Book.Builder book_builder;
  private final DownloaderType downloader;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;
  private long download_running_total;
  private BookDatabaseEntryType database_entry;

  BookBorrowTask(
      final DownloaderType downloader,
      final ConcurrentHashMap<BookID, DownloadType> downloads,
      final FeedLoaderType feed_loader,
      final BundledContentResolverType bundled_content,
      final BookRegistryType book_registry,
      final BookID id,
      final AccountType account,
      final OPDSAcquisition acquisition,
      final OPDSAcquisitionFeedEntry entry) {

    this.downloader =
        NullCheck.notNull(downloader, "Downloader");
    this.downloads =
        NullCheck.notNull(downloads, "Downloads");
    this.feed_loader =
        NullCheck.notNull(feed_loader, "Feed loader");
    this.bundled_content =
        NullCheck.notNull(bundled_content, "bundled_content");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
    this.book_id =
        NullCheck.notNull(id, "ID");
    this.account =
        NullCheck.notNull(account, "Account");
    this.acquisition =
        NullCheck.notNull(acquisition, "Acquisition");
    this.entry =
        NullCheck.notNull(entry, "Entry");

    this.book_builder = Book.builder(this.book_id, this.account.id(), this.entry);
  }

  @Override
  public Unit call() throws Exception {
    execute();
    return Unit.unit();
  }

  private void execute() {

    try {
      LOG.debug("[{}]: starting borrow", this.book_id.brief());
      LOG.debug("[{}]: creating feed entry", this.book_id.brief());

      this.book_registry.update(
          BookWithStatus.create(this.book_builder.build(),
              new BookStatusRequestingLoan(this.book_id)));

      final BookDatabaseType database = this.account.bookDatabase();
      this.database_entry = database.createOrUpdate(this.book_id, this.entry);

      if (BundledURIs.isBundledURI(this.acquisition.getUri())) {
        LOG.debug("[{}]: acquisition is bundled", this.book_id.brief());
        this.runAcquisitionBundled();
        return;
      }

      final OPDSAcquisition.Relation type = this.acquisition.getRelation();
      switch (type) {
        case ACQUISITION_BORROW: {
          LOG.debug("[{}]: acquisition type is {}, performing borrow", this.book_id.brief(), type);
          this.runAcquisitionBorrow();
          return;
        }
        case ACQUISITION_GENERIC: {
          LOG.debug("[{}]: acquisition type is {}, performing generic procedure", this.book_id.brief(), type);
          this.runAcquisitionGeneric();
          return;
        }
        case ACQUISITION_OPEN_ACCESS: {
          LOG.debug("[{}]: acquisition type is {}, performing fulfillment", this.book_id.brief(), type);
          this.downloadAddToCurrent(this.runAcquisitionFulfill(this.entry));
          return;
        }
        case ACQUISITION_BUY:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE: {
          LOG.debug("[{}]: acquisition type is {}, cannot continue!", this.book_id.brief(), type);
          throw new UnsupportedOperationException();
        }
      }
    } catch (final Exception e) {
      LOG.error("[{}]: error: ", this.book_id.brief(), e);
      this.downloadFailed(Option.some(e));
    }
  }

  /**
   * Copy data out of the bundled resources.
   */

  private void runAcquisitionBundled() throws IOException, BookDatabaseException {

    final File file = this.database_entry.temporaryFile();
    final byte[] buffer = new byte[2048];

    try (OutputStream output = new FileOutputStream(file)) {
      try (InputStream stream = this.bundled_content.resolve(this.acquisition.getUri())) {
        final long size = stream.available();
        long consumed = 0L;
        this.downloadDataReceived(consumed, size);

        while (true) {
          final int r = stream.read(buffer);
          if (r == -1) {
            break;
          }
          consumed += r;
          output.write(buffer, 0, r);
          this.downloadDataReceived(consumed, size);
        }
        output.flush();
      }

      this.saveEPUBAndRights(file, Option.none());
      final Book book = this.database_entry.book();
      this.book_registry.update(BookWithStatus.create(book, BookStatus.fromBook(book)));
    } catch (final IOException | BookDatabaseException e) {
      FileUtilities.fileDelete(file);
      throw e;
    }
  }

  private void runAcquisitionGeneric()
      throws NoUsableAcquisitionException {

    /*
     * The feed requires DRM support...
     */

    if (this.account.provider().authentication().isSome()) {
      throw new UnsupportedOperationException();
    }

    /*
     * The feed doesn't require DRM support...
     */

    LOG.debug("[{}]: performing fulfillment of generic acquisition", this.book_id.brief());
    this.runAcquisitionFulfill(this.entry);
  }

  private void runAcquisitionBorrow() throws AuthenticationRequiredException {
    LOG.debug("[{}]: borrowing", this.book_id.brief());

    /*
     * Borrowing requires authentication.
     */

    final OptionType<AccountAuthenticationCredentials> credentials_opt = this.account.credentials();
    if (!credentials_opt.isSome()) {
      throw new AuthenticationRequiredException();
    }

    final AccountAuthenticationCredentials credentials =
        ((Some<AccountAuthenticationCredentials>) credentials_opt).get();
    final HTTPAuthType auth =
        AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials);

    /*
     * Grab the feed for the borrow link.
     */

    LOG.debug("[{}]: fetching item feed: {}", this.book_id.brief(), this.acquisition.getUri());

    this.feed_loader.fromURIRefreshing(
        this.acquisition.getUri(),
        Option.some(auth),
        "PUT",
        new FeedListener(this));
  }

  private DownloadType runAcquisitionFulfill(
      final OPDSAcquisitionFeedEntry entry)
      throws NoUsableAcquisitionException {

    LOG.debug("[{}]: fulfilling book", this.book_id.brief());

    for (final OPDSAcquisition acquisition : entry.getAcquisitions()) {
      switch (acquisition.getRelation()) {
        case ACQUISITION_GENERIC:
        case ACQUISITION_OPEN_ACCESS: {
          return this.runAcquisitionFulfillDoDownload(acquisition);
        }
        case ACQUISITION_BORROW:
        case ACQUISITION_BUY:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE: {
          break;
        }
      }
    }

    throw new NoUsableAcquisitionException();
  }

  private DownloadType runAcquisitionFulfillDoDownload(final OPDSAcquisition acquisition) {

    /*
     * Downloading may require authentication.
     */

    final OptionType<HTTPAuthType> auth =
        this.account.credentials().map(AccountAuthenticatedHTTP::createAuthenticatedHTTP);

    LOG.debug("[{}]: starting download", this.book_id.brief());

    /*
     * Point the downloader at the acquisition link. The result will either
     * be an EPUB or an ACSM file. ACSM files have to be "fulfilled" after
     * downloading by passing them to the Adobe DRM connector.
     */

    return this.downloader.download(acquisition.getUri(), auth, new FulfillmentListener(this));
  }

  private void runAcquisitionBorrowGotOPDSEntry(
      final FeedEntryOPDS opds_entry)
      throws BookDatabaseException, NoUsableAcquisitionException {

    LOG.debug("[{}]: received OPDS feed entry", this.book_id.brief());
    final OPDSAcquisitionFeedEntry ee = opds_entry.getFeedEntry();
    final OPDSAvailabilityType availability = ee.getAvailability();
    LOG.debug("[{}]: book availability is {}", this.book_id.brief(), availability);

    /*
     * Update the database.
     */

    LOG.debug("[{}]: saving state to database", this.book_id.brief());
    final BookDatabaseEntryType db_e = this.account.bookDatabase().entry(this.book_id);
    db_e.writeOPDSEntry(ee);

    /*
     * Then, work out what to do based on the latest availability data.
     * If the book is loaned, attempt to download it. If it is held, notify
     * the user.
     */

    LOG.debug("[{}]: continuing borrow based on availability", this.book_id.brief());

    final Boolean want_fulfill =
        availability.matchAvailability(new WantFulfillmentChecker(this));

    if (want_fulfill) {
      this.downloadAddToCurrent(this.runAcquisitionFulfill(ee));
    }
  }

  private void downloadFailed(final OptionType<Throwable> exception) {
    LogUtilities.errorWithOptionalException(LOG, "download failed", exception);

    this.book_registry.update(
        BookWithStatus.create(this.book_builder.build(),
            new BookStatusDownloadFailed(this.book_id, exception, Option.none())));
  }

  private static final class NoUsableAcquisitionException extends Exception {

  }

  private static final class AuthenticationRequiredException extends Exception {

  }

  private static final class FetchingACSMFailed extends Exception {
    FetchingACSMFailed(final OptionType<Throwable> exception) {
      super(exception.accept(new OptionVisitorType<Throwable, Throwable>() {
        @Override
        public Throwable none(final None<Throwable> none) {
          return null;
        }

        @Override
        public Throwable some(final Some<Throwable> some) {
          return some.get();
        }
      }));
    }
  }

  private static final class FetchingBookFailed extends Exception {
    FetchingBookFailed(final OptionType<Throwable> exception) {
      super(exception.accept(new OptionVisitorType<Throwable, Throwable>() {
        @Override
        public Throwable none(final None<Throwable> none) {
          return null;
        }

        @Override
        public Throwable some(final Some<Throwable> some) {
          return some.get();
        }
      }));
    }
  }

  private void downloadDataReceived(
      final long running_total,
      final long expected_total) {

    /*
     * Because "data received" updates happen at such a huge rate, we want
     * to ensure that updates to the book status are rate limited to avoid
     * overwhelming the UI. Book updates are only published at the start of
     * downloads, or if a large enough chunk of data has now been received
     * to justify a UI update.
     */

    final boolean at_start = running_total == 0L;
    final double divider = (double) expected_total / 10.0;
    final boolean long_enough =
        (double) running_total > (double) this.download_running_total + divider;

    if (long_enough || at_start) {
      this.book_registry.update(
          BookWithStatus.create(
              this.book_builder.build(),
              new BookStatusDownloadInProgress(
                  this.book_id, running_total, expected_total, Option.none())));
      this.download_running_total = running_total;
    }
  }

  private static final class FulfillmentListener implements DownloadListenerType {

    private final BookBorrowTask task;

    FulfillmentListener(final BookBorrowTask task) {
      this.task = NullCheck.notNull(task, "Task");
    }

    @Override
    public void onDownloadStarted(
        final DownloadType d,
        final long expected_total) {
      this.task.downloadDataReceived(0L, expected_total);
    }

    @Override
    public void onDownloadDataReceived(
        final DownloadType d,
        final long running_total,
        final long expected_total) {
      this.task.downloadDataReceived(running_total, expected_total);
    }

    @Override
    public void onDownloadCancelled(final DownloadType d) {
      this.task.downloadCancelled();
    }

    @Override
    public void onDownloadFailed(
        final DownloadType d,
        final int status,
        final long running_total,
        final OptionType<Throwable> exception) {

      /*
       * If the content type indicates that the file was an ACSM file,
       * explicitly indicate that it was fetching an ACSM that failed.
       * This allows the UI to assign blame!
       */

      final Throwable ex;
      final String acsm_type = ACSM_CONTENT_TYPE;
      if (acsm_type.equals(d.getContentType())) {
        ex = new FetchingACSMFailed(exception);
      } else {
        ex = new FetchingBookFailed(exception);
      }

      this.task.downloadFailed(Option.some(ex));
    }

    @Override
    public void onDownloadCompleted(
        final DownloadType d,
        final File file) throws IOException {
      this.task.downloadCompleted(d, file);
    }
  }

  private void downloadCompleted(
      final DownloadType download,
      final File file) {

    try {
      LOG.debug("[{}]: download {} completed for {}", this.book_id.brief(), download, file);
      this.downloadRemoveFromCurrent();

      /*
       * If the downloaded file is an ACSM fulfillment token, then the
       * book must be downloaded using the Adobe DRM interface.
       */

      final String content_type = download.getContentType();
      LOG.debug("[{}]: content type is {}", this.book_id.brief(), content_type);

      if (ACSM_CONTENT_TYPE.equals(content_type)) {
        this.runFulfillACSM(file);
        return;
      }

      /*
       * Otherwise, assume it's an EPUB and keep it.
       */

      final OptionType<AdobeAdeptLoan> none = Option.none();
      this.saveEPUBAndRights(file, none);

      try {
        final Book book = this.database_entry.book();
        this.book_registry.update(BookWithStatus.create(book, BookStatus.fromBook(book)));
      } finally {
        this.downloadRemoveFromCurrent();
      }

    } catch (final Exception e) {
      LOG.error("onDownloadCompleted: exception: ", e);
      this.downloadFailed(Option.some(e));
    }
  }

  private void downloadAddToCurrent(final DownloadType download) {
    LOG.debug("[{}]: adding download {}", this.book_id.brief(), download);
    this.downloads.put(this.book_id, download);
  }

  private void downloadRemoveFromCurrent() {
    LOG.debug("[{}]: removing download", this.book_id.brief());
    this.downloads.remove(this.book_id);
  }

  private void runFulfillACSM(final File file) throws AdobeAdeptACSMException {
    throw new UnimplementedCodeException();
  }

  private void saveEPUBAndRights(
      final File file,
      final OptionType<AdobeAdeptLoan> loan_opt)
      throws BookDatabaseException {

    this.database_entry.writeEPUB(file);
    loan_opt.mapPartial_(loan -> this.database_entry.writeAdobeLoan(loan));
  }

  private void downloadCancelled() {
    try {
      final Book book = this.database_entry.book();
      this.book_registry.update(BookWithStatus.create(book, BookStatus.fromBook(book)));
    } finally {
      this.downloadRemoveFromCurrent();
    }
  }

  private static final class FeedListener
      implements FeedLoaderListenerType,
      FeedMatcherType<Unit, UnreachableCodeException>,
      FeedEntryMatcherType<Unit, UnreachableCodeException> {

    private final BookBorrowTask task;

    FeedListener(final BookBorrowTask task) {
      this.task = NullCheck.notNull(task, "Task");
    }

    @Override
    public void onFeedLoadSuccess(
        final URI u,
        final FeedType f) {

      try {
        LOG.debug("[{}]: loaded feed from {}", task.book_id.brief(), u);
        f.matchFeed(this);
      } catch (final Throwable e) {
        LOG.error("[{}]: failure after receiving feed: {}: ", task.book_id.brief(), u, e);
        this.task.downloadFailed(Option.some(e));
      }
    }

    @Override
    public void onFeedRequiresAuthentication(
        final URI uri,
        final int attempts,
        final FeedLoaderAuthenticationListenerType listener) {

      LOG.debug("[{}]: feed {} requires authentication but none can be provided",
          this.task.book_id.brief(),
          uri);

    }

    @Override
    public void onFeedLoadFailure(
        final URI u,
        final Throwable x) {

      LOG.debug("[{}]: failed to load feed", this.task.book_id.brief());

      Throwable ex = new BookBorrowExceptionFetchingBorrowFeedFailed(x);
      if (x instanceof OPDSParseException) {
        ex = new BookBorrowExceptionBadBorrowFeed(x);
      } else if (x instanceof FeedHTTPTransportException) {
        final OptionType<HTTPProblemReport> problem_report_opt =
            ((FeedHTTPTransportException) x).getProblemReport();
        if (problem_report_opt.isSome()) {
          final HTTPProblemReport problem_report =
              ((Some<HTTPProblemReport>) problem_report_opt).get();
          final HTTPProblemReport.ProblemType problem_type =
              problem_report.getProblemType();

          switch (problem_type) {
            case LoanLimitReached: {
              ex = new BookBorrowExceptionLoanLimitReached(x);
              break;
            }
            case Unknown: {
              break;
            }
          }
        }
      }

      this.task.downloadFailed(Option.some(ex));
    }

    @Override
    public Unit onFeedWithGroups(final FeedWithGroups f) {
      LOG.debug("[{}]: received feed with groups, using first entry", this.task.book_id.brief());
      final FeedGroup g = NullCheck.notNull(f.get(0));
      final FeedEntryType e = NullCheck.notNull(g.getGroupEntries().get(0));
      return e.matchFeedEntry(this);
    }

    @Override
    public Unit onFeedWithoutGroups(final FeedWithoutGroups f) {
      LOG.debug("[{}]: received feed without groups, using first entry", this.task.book_id.brief());
      final FeedEntryType e = NullCheck.notNull(f.get(0));
      return e.matchFeedEntry(this);
    }

    @Override
    public Unit onFeedEntryOPDS(final FeedEntryOPDS e) {
      try {
        this.task.runAcquisitionBorrowGotOPDSEntry(e);
      } catch (final BookDatabaseException | NoUsableAcquisitionException x) {
        this.task.downloadFailed(Option.some(x));
      }
      return Unit.unit();
    }

    @Override
    public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e) {
      LOG.error("[{}]: unexpectedly received corrupt feed entry", this.task.book_id.brief());
      this.task.downloadFailed(Option.some(new BookBorrowExceptionBadBorrowFeed(e.getError())));
      return Unit.unit();
    }
  }

  private static class WantFulfillmentChecker implements OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException> {

    private final BookBorrowTask task;

    WantFulfillmentChecker(final BookBorrowTask task) {
      this.task = NullCheck.notNull(task, "Task");
    }

    /**
     * If the book is held but is ready for download, just notify
     * the user of this fact by setting the status.
     */

    @Override
    public Boolean onHeldReady(final OPDSAvailabilityHeldReady a) {
      LOG.debug("[{}]: book is held but is ready, nothing more to do", this.task.book_id.brief());

      final BookStatusHeldReady status =
          new BookStatusHeldReady(this.task.book_id, a.getEndDate(), a.getRevoke().isSome());

      this.task.book_registry.update(BookWithStatus.create(this.task.book_builder.build(), status));
      return Boolean.FALSE;
    }

    /**
     * If the book is held, just notify the user of this fact by
     * setting the status.
     */

    @Override
    public Boolean onHeld(final OPDSAvailabilityHeld a) {
      LOG.debug("[{}]: book is held, nothing more to do", this.task.book_id.brief());

      final BookStatusHeld status =
          new BookStatusHeld(
              this.task.book_id,
              a.getPosition(),
              a.getStartDate(),
              a.getEndDate(),
              a.getRevoke().isSome());

      this.task.book_registry.update(BookWithStatus.create(this.task.book_builder.build(), status));
      return Boolean.FALSE;
    }

    /**
     * If the book is available to be placed on hold, set the
     * appropriate status.
     * <p>
     * XXX: This should not occur in practice! Should this code be
     * unreachable?
     */

    @Override
    public Boolean onHoldable(final OPDSAvailabilityHoldable a) {
      LOG.debug("[{}]: book is holdable, cannot continue!", this.task.book_id.brief());

      final BookStatusHoldable status = new BookStatusHoldable(this.task.book_id);
      this.task.book_registry.update(BookWithStatus.create(this.task.book_builder.build(), status));
      return Boolean.FALSE;
    }

    /**
     * If the book claims to be only "loanable", then something is
     * definitely wrong.
     * <p>
     * XXX: This should not occur in practice! Should this code be
     * unreachable?
     */

    @Override
    public Boolean onLoanable(final OPDSAvailabilityLoanable a) {
      LOG.debug("[{}]: book is loanable, this is a server bug!", this.task.book_id.brief());

      throw new UnreachableCodeException();
    }

    /**
     * If the book is "loaned", then attempt to fulfill the book.
     */

    @Override
    public Boolean onLoaned(final OPDSAvailabilityLoaned a) {
      LOG.debug("[{}]: book is loaned, fulfilling", this.task.book_id.brief());

      final BookStatusRequestingDownload status =
          new BookStatusRequestingDownload(this.task.book_id, a.getEndDate());
      this.task.book_registry.update(BookWithStatus.create(this.task.book_builder.build(), status));
      return Boolean.TRUE;
    }

    /**
     * If the book is "open-access", then attempt to fulfill the
     * book.
     */

    @Override
    public Boolean onOpenAccess(final OPDSAvailabilityOpenAccess a) {
      LOG.debug("[{}]: book is open access, fulfilling", this.task.book_id.brief());

      final OptionType<Calendar> none = Option.none();
      final BookStatusRequestingDownload status =
          new BookStatusRequestingDownload(this.task.book_id, none);
      this.task.book_registry.update(BookWithStatus.create(this.task.book_builder.build(), status));
      return Boolean.TRUE;
    }

    /**
     * The server cannot return a "revoked" representation. Reaching
     * this code indicates a serious bug in the application.
     */

    @Override
    public Boolean onRevoked(final OPDSAvailabilityRevoked a) {
      throw new UnreachableCodeException();
    }
  }
}
