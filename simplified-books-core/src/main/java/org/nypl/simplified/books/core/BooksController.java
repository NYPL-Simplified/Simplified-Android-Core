package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultToException;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The default implementation of the {@link BooksType} interface.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class BooksController extends
  Observable implements BooksType
{
  private static final class BorrowTask implements
    Runnable,
    DownloadListenerType
  {
    private final OPDSAcquisition        acq;
    private final BookID                 book_id;
    private final BookDatabaseType       books_database;
    private final BooksStatusCacheType   books_status;
    private final DownloaderType         downloader;
    private final BookBorrowListenerType listener;
    private final String                 title;

    public BorrowTask(
      final BookDatabaseType in_books_database,
      final BooksStatusCacheType in_books_status,
      final DownloaderType in_downloader,
      final BookID in_book_id,
      final OPDSAcquisition in_acq,
      final String in_title,
      final BookBorrowListenerType in_listener)
    {
      this.downloader = NullCheck.notNull(in_downloader);
      this.book_id = NullCheck.notNull(in_book_id);
      this.acq = NullCheck.notNull(in_acq);
      this.listener = NullCheck.notNull(in_listener);
      this.books_database = NullCheck.notNull(in_books_database);
      this.books_status = NullCheck.notNull(in_books_status);
      this.title = NullCheck.notNull(in_title);
    }

    @Override public void downloadCancelled(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadCancelled status =
        new BookStatusDownloadCancelled(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadCleanedUp(
      final DownloadSnapshot snap)
    {
      // Don't care
    }

    @Override public void downloadCompleted(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadInProgress status =
        new BookStatusDownloadInProgress(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadCompletedTake(
      final DownloadSnapshot snap,
      final File file_data)
    {
      try {
        final BookDatabaseEntryType e =
          this.books_database.getBookDatabaseEntry(this.book_id);

        e.copyInBookFromSameFilesystem(file_data);
        final BookStatusDownloaded status =
          new BookStatusDownloaded(this.book_id);
        this.books_status.booksSnapshotUpdate(this.book_id, e.getSnapshot());
        this.books_status.booksStatusUpdate(status);
      } catch (final IOException e) {
        throw new IOError(e);
      }
    }

    @Override public void downloadCompletedTakeFailed(
      final DownloadSnapshot snap,
      final Throwable x)
    {
      final BookStatusDownloadFailed status =
        new BookStatusDownloadFailed(this.book_id, snap, Option.some(x));
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadCompletedTaken(
      final DownloadSnapshot snap)
    {
      // Don't care
    }

    @Override public void downloadFailed(
      final DownloadSnapshot snap,
      final Throwable e)
    {
      final BookStatusDownloadFailed status =
        new BookStatusDownloadFailed(this.book_id, snap, Option.some(e));
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadPaused(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadingPaused status =
        new BookStatusDownloadingPaused(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadReceivedData(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadInProgress status =
        new BookStatusDownloadInProgress(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadResumed(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadInProgress status =
        new BookStatusDownloadInProgress(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadStarted(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadInProgress status =
        new BookStatusDownloadInProgress(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void downloadStartedReceivingData(
      final DownloadSnapshot snap)
    {
      final BookStatusDownloadInProgress status =
        new BookStatusDownloadInProgress(this.book_id, snap);
      this.books_status.booksStatusUpdate(status);
    }

    @Override public void run()
    {
      try {
        this.runAcquisition();
        this.listener.onBookBorrowSuccess(this.book_id);
      } catch (final Throwable x) {
        this.listener.onBookBorrowFailure(this.book_id, Option.some(x));
      }
    }

    private void runAcquisition()
      throws Exception
    {
      switch (this.acq.getType()) {
        case ACQUISITION_GENERIC:
        case ACQUISITION_BORROW:
        {
          this.runAcquisitionBorrow();
          break;
        }

        case ACQUISITION_OPEN_ACCESS:
        case ACQUISITION_BUY:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE:
        {
          throw new UnimplementedCodeException();
        }
      }
    }

    private void runAcquisitionBorrow()
      throws Exception
    {
      BooksController.LOG.debug(
        "book {}: creating book database entry",
        this.book_id);

      final BookDatabaseEntryType e =
        this.books_database.getBookDatabaseEntry(this.book_id);
      e.create();

      BooksController.LOG.debug("book {}: starting download", this.book_id);

      final Pair<AccountBarcode, AccountPIN> p =
        this.books_database.credentialsGet();
      final AccountBarcode barcode = p.getLeft();
      final AccountPIN pin = p.getRight();
      final HTTPAuthType auth =
        new HTTPAuthBasic(barcode.toString(), pin.toString());

      final long did =
        this.downloader.downloadEnqueue(
          Option.some(auth),
          this.acq.getURI(),
          this.title,
          this);

      BooksController.LOG.debug("book {}: download id {}", this.book_id, did);
      e.setDownloadID(did);
    }
  }

  private static final class DataLoadTask implements Runnable
  {
    private final BookDatabaseType                                  books_database;
    private final BooksStatusCacheType                              books_status;
    private final DownloaderType                                    downloader;
    private final AccountDataLoadListenerType                       listener;
    private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;

    public DataLoadTask(
      final BookDatabaseType in_books_database,
      final BooksStatusCacheType in_books_status,
      final DownloaderType in_downloader,
      final AccountDataLoadListenerType in_listener,
      final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login)
    {
      this.books_database = NullCheck.notNull(in_books_database);
      this.books_status = NullCheck.notNull(in_books_status);
      this.downloader = NullCheck.notNull(in_downloader);
      this.listener = NullCheck.notNull(in_listener);
      this.login = NullCheck.notNull(in_login);
    }

    private void loadBooks()
    {
      final List<BookDatabaseEntryType> book_list =
        this.books_database.getBookDatabaseEntries();
      for (final BookDatabaseEntryReadableType book_dir : book_list) {
        final BookID id = book_dir.getID();
        try {
          final BookSnapshot snap = book_dir.getSnapshot();
          final BookStatusLoanedType status =
            BookStatus.fromBookSnapshot(this.downloader, id, snap);

          this.books_status.booksStatusUpdate(status);
          this.books_status.booksSnapshotUpdate(id, snap);
          this.listener.onAccountDataBookLoadSucceeded(id, snap);
        } catch (final Throwable e) {
          this.listener.onAccountDataBookLoadFailed(
            id,
            Option.some(e),
            e.getMessage());
        }
      }
    }

    @Override public void run()
    {
      if (this.books_database.credentialsExist()) {
        try {
          this.login.set(this.books_database.credentialsGet());
        } catch (final IOException e) {
          try {
            this.listener.onAccountDataLoadFailedImmediately(e);
          } catch (final Throwable x) {
            BooksController.LOG.error("listener raised exception: ", x);
          }
        }

        this.loadBooks();
      } else {
        try {
          this.listener.onAccountUnavailable();
        } catch (final Throwable x) {
          BooksController.LOG.error("listener raised exception: ", x);
        }
      }

      this.listener.onAccountDataBookLoadFinished();
    }
  }

  private static final class DataSetupTask implements Runnable
  {
    private final BookDatabaseType             books_database;
    private final BooksControllerConfiguration config;
    private final AccountDataSetupListenerType listener;

    public DataSetupTask(
      final BooksControllerConfiguration in_config,
      final BookDatabaseType in_books_database,
      final AccountDataSetupListenerType in_listener)
    {
      this.books_database = NullCheck.notNull(in_books_database);
      this.config = NullCheck.notNull(in_config);
      this.listener = NullCheck.notNull(in_listener);
    }

    @Override public void run()
    {
      try {
        this.books_database.create();
        this.listener.onAccountDataSetupSuccess();
      } catch (final Throwable x) {
        this.listener.onAccountDataSetupFailure(
          Option.some(x),
          x.getMessage());
      }
    }
  }

  private static final class DeleteBookDataTask implements Runnable
  {
    private final BookDatabaseType     book_database;
    private final BookID               book_id;
    private final BooksStatusCacheType books_status;

    public DeleteBookDataTask(
      final BooksStatusCacheType in_books_status,
      final BookDatabaseType in_book_database,
      final BookID in_book_id)
    {
      this.books_status = NullCheck.notNull(in_books_status);
      this.book_database = NullCheck.notNull(in_book_database);
      this.book_id = NullCheck.notNull(in_book_id);
    }

    @Override public void run()
    {
      try {
        final BookDatabaseEntryType e =
          this.book_database.getBookDatabaseEntry(this.book_id);
        e.destroyBookData();

        this.books_status
          .booksStatusUpdate(new BookStatusLoaned(this.book_id));
      } catch (final Throwable e) {
        BooksController.LOG.error(
          "could not destroy book data for {}: ",
          this.book_id,
          e);
      }
    }
  }

  private static final class FeedTask implements Runnable
  {
    private final BookDatabaseType     books_database;
    private final String               id;
    private final BookFeedListenerType listener;
    private final String               title;
    private final Calendar             updated;
    private final URI                  uri;

    public FeedTask(
      final BookDatabaseType in_books_database,
      final URI in_uri,
      final String in_id,
      final Calendar in_updated,
      final String in_title,
      final BookFeedListenerType in_listener)
    {
      this.books_database = NullCheck.notNull(in_books_database);
      this.uri = NullCheck.notNull(in_uri);
      this.id = NullCheck.notNull(in_id);
      this.updated = NullCheck.notNull(in_updated);
      this.title = NullCheck.notNull(in_title);
      this.listener = NullCheck.notNull(in_listener);
    }

    private FeedWithoutBlocks feed()
    {
      final OptionType<URI> no_next = Option.none();
      final OptionType<OPDSSearchLink> no_search = Option.none();
      final FeedWithoutBlocks f =
        FeedWithoutBlocks.newEmptyFeed(
          this.uri,
          this.id,
          this.updated,
          this.title,
          no_next,
          no_search);

      final List<BookDatabaseEntryType> dirs =
        this.books_database.getBookDatabaseEntries();

      for (int index = 0; index < dirs.size(); ++index) {
        final BookDatabaseEntryReadableType dir =
          NullCheck.notNull(dirs.get(index));
        final BookID book_id = dir.getID();

        try {
          final OPDSAcquisitionFeedEntry data = dir.getData();
          f.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(data));
        } catch (final IOException x) {
          BooksController.LOG.error(
            "unable to load book {} metadata: ",
            book_id,
            x);
          f.add(FeedEntryCorrupt.fromIDAndError(book_id, x));
        }
      }

      return f;
    }

    @Override public void run()
    {
      try {
        this.listener.onBookFeedSuccess(this.feed());
      } catch (final Throwable x) {
        this.listener.onBookFeedFailure(x);
      }
    }
  }

  private static final class LoginTask implements
    Runnable,
    AccountDataSetupListenerType
  {
    private final AccountBarcode                                    barcode;
    private final BooksController                                   books;
    private final BookDatabaseType                                  books_database;
    private final BooksControllerConfiguration                      config;
    private final HTTPType                                          http;
    private final AccountLoginListenerType                          listener;
    private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;
    private final AccountPIN                                        pin;

    public LoginTask(
      final BooksController in_books,
      final BookDatabaseType in_books_database,
      final HTTPType in_http,
      final BooksControllerConfiguration in_config,
      final AccountBarcode in_barcode,
      final AccountPIN in_pin,
      final AccountLoginListenerType in_listener,
      final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login)
    {
      this.books = NullCheck.notNull(in_books);
      this.books_database = NullCheck.notNull(in_books_database);
      this.http = NullCheck.notNull(in_http);
      this.config = NullCheck.notNull(in_config);
      this.barcode = NullCheck.notNull(in_barcode);
      this.pin = NullCheck.notNull(in_pin);
      this.listener = NullCheck.notNull(in_listener);
      this.login = NullCheck.notNull(in_login);
    }

    private void loginCheckCredentials()
      throws Exception
    {
      final HTTPAuthType auth =
        new HTTPAuthBasic(this.barcode.toString(), this.pin.toString());
      final HTTPResultType<Unit> r =
        this.http.head(Option.some(auth), this.config.getLoansURI());

      r.matchResult(new ResultAuthExceptional<Unit>() {
        @Override public HTTPResultOKType<Unit> onHTTPOK(
          final HTTPResultOKType<Unit> e)
          throws Exception
        {
          /**
           * Credentials were accepted, write them to files.
           */

          LoginTask.this.saveCredentials(LoginTask.this.pin);
          LoginTask.this.login.set(Pair.pair(
            LoginTask.this.barcode,
            LoginTask.this.pin));
          return e;
        }
      });
    }

    @Override public void onAccountDataSetupFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      this.listener.onAccountLoginFailure(error, message);
    }

    @Override public void onAccountDataSetupSuccess()
    {
      try {
        this.loginCheckCredentials();
        this.listener.onAccountLoginSuccess(this.barcode, this.pin);
      } catch (final Throwable e) {
        this.listener.onAccountLoginFailure(Option.some(e), e.getMessage());
      }
    }

    @Override public void run()
    {
      this.books.submitRunnable(new DataSetupTask(
        this.config,
        this.books_database,
        this));
    }

    private void saveCredentials(
      final AccountPIN actual_pin)
      throws IOException
    {
      this.books_database.credentialsSet(this.barcode, actual_pin);
    }
  }

  private static final class LogoutTask implements Runnable
  {
    private final File                                              base;
    private final BooksControllerConfiguration                      config;
    private final AccountLogoutListenerType                         listener;
    private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;

    public LogoutTask(
      final BooksControllerConfiguration in_config,
      final AtomicReference<Pair<AccountBarcode, AccountPIN>> in_login,
      final AccountLogoutListenerType in_listener)
    {
      this.config = NullCheck.notNull(in_config);
      this.listener = NullCheck.notNull(in_listener);
      this.login = NullCheck.notNull(in_login);
      this.base = new File(this.config.getDirectory(), "data");
    }

    @Override public void run()
    {
      try {
        this.login.set(null);

        if (this.base.isDirectory()) {
          DirectoryUtilities.directoryDelete(this.base);
        } else {
          throw new IllegalStateException("Not logged in");
        }

        this.listener.onAccountLogoutSuccess();
      } catch (final Throwable e) {
        this.listener.onAccountLogoutFailure(Option.some(e), e.getMessage());
      }
    }
  }

  private static abstract class ResultAuthExceptional<A> implements
    HTTPResultMatcherType<A, HTTPResultOKType<A>, Exception>
  {
    public ResultAuthExceptional()
    {

    }

    @Override public final HTTPResultOKType<A> onHTTPError(
      final HTTPResultError<A> e)
      throws Exception
    {
      final String m =
        NullCheck.notNull(String.format(
          "%d: %s",
          e.getStatus(),
          e.getMessage()));

      switch (e.getStatus()) {
        case HttpURLConnection.HTTP_UNAUTHORIZED:
        {
          throw new AccountAuthenticationPINRejectedError(
            "Invalid barcode or PIN");
        }
        default:
        {
          throw new IOException(m);
        }
      }
    }

    @Override public final HTTPResultOKType<A> onHTTPException(
      final HTTPResultException<A> e)
      throws Exception
    {
      throw e.getError();
    }
  }

  private static final class SyncTask implements Runnable
  {

    private final BookDatabaseType             books_database;
    private final BooksControllerConfiguration config;
    private final DownloaderType               downloader;
    private final OPDSFeedParserType           feed_parser;
    private final HTTPType                     http;
    private final AccountSyncListenerType      listener;
    private final BooksStatusCacheType         status_cache;

    public SyncTask(
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
      this.downloader = NullCheck.notNull(in_downloader);
    }

    @Override public void run()
    {
      try {
        this.sync();
        this.listener.onAccountSyncSuccess();
      } catch (final Throwable x) {
        this.listener.onAccountSyncFailure(Option.some(x), x.getMessage());
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
        this.http.get(Option.some(auth), this.config.getLoansURI(), 0);

      r
        .matchResult(new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override public Unit onHTTPError(
            final HTTPResultError<InputStream> e)
            throws Exception
          {
            final String m =
              NullCheck.notNull(String.format(
                "%d: %s",
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
              SyncTask.this.syncFeedEntries(loans_uri, e);
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
      final OPDSFeedType feed =
        this.feed_parser.parse(loans_uri, r_feed.getValue());

      if (feed instanceof OPDSNavigationFeed) {
        throw new IOException(
          "Expected an acquisition feed, but received a navigation feed");
      }

      final OPDSAcquisitionFeed acq_feed = (OPDSAcquisitionFeed) feed;
      final List<OPDSAcquisitionFeedEntry> entries =
        acq_feed.getFeedEntries();

      for (final OPDSAcquisitionFeedEntry e : entries) {
        this.syncFeedEntry(NullCheck.notNull(e));
      }
    }

    private void syncFeedEntry(
      final OPDSAcquisitionFeedEntry e)
      throws Exception
    {
      final BookID book_id = BookID.newIDFromEntry(e);
      final BookDatabaseEntryType book_dir =
        new BookDatabaseEntry(this.books_database.getLocation(), book_id);

      final OptionType<File> cover =
        BooksController.makeCover(this.http, e.getCover());

      book_dir.create();
      book_dir.setData(cover, e);

      final BookSnapshot snap = book_dir.getSnapshot();
      final BookStatusLoanedType status =
        BookStatus.fromBookSnapshot(this.downloader, book_id, snap);

      this.status_cache.booksStatusUpdateIfMoreImportant(status);
      this.status_cache.booksSnapshotUpdate(book_id, snap);
      this.listener.onAccountSyncBook(book_id);
    }
  }

  private static final class UpdateMetadataTask implements Runnable
  {
    private final BookID                   book_id;
    private final BookDatabaseType         books_database;
    private final OPDSAcquisitionFeedEntry entry;
    private final HTTPType                 http;

    public UpdateMetadataTask(
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

        final OptionType<File> cover =
          BooksController.makeCover(this.http, this.entry.getCover());

        e.create();
        e.setData(cover, this.entry);
      } catch (final Exception e) {
        BooksController.LOG.error("unable to update metadata: ", e);
      }
    }
  }

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksController.class));
  }

  private static OptionType<File> makeCover(
    final HTTPType http,
    final OptionType<URI> cover_opt)
    throws Exception
  {
    if (cover_opt.isSome()) {
      final Some<URI> some = (Some<URI>) cover_opt;
      final URI cover_uri = some.get();

      final File cover_file_tmp = File.createTempFile("cover", ".jpg");
      cover_file_tmp.deleteOnExit();
      BooksController.makeCoverDownload(http, cover_file_tmp, cover_uri);
      return Option.some(cover_file_tmp);
    }

    return Option.none();
  }

  private static void makeCoverDownload(
    final HTTPType http,
    final File cover_file_tmp,
    final URI cover_uri)
    throws Exception
  {
    final OptionType<HTTPAuthType> no_auth = Option.none();
    final HTTPResultOKType<InputStream> r =
      http.get(no_auth, cover_uri, 0).matchResult(
        new HTTPResultToException<InputStream>());

    try {
      final FileOutputStream fs = new FileOutputStream(cover_file_tmp);
      try {
        final InputStream in = NullCheck.notNull(r.getValue());
        final byte[] buffer = new byte[8192];
        for (;;) {
          final int rb = in.read(buffer);
          if (rb == -1) {
            break;
          }
          fs.write(buffer, 0, rb);
        }

        fs.flush();
      } finally {
        fs.close();
      }
    } finally {
      r.close();
    }
  }

  public static BooksType newBooks(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final BooksControllerConfiguration in_config)
  {
    return new BooksController(
      in_exec,
      in_feeds,
      in_http,
      in_downloader,
      in_config);
  }

  private final BookDatabaseType                                  book_database;
  private final BooksStatusCacheType                              books_status;
  private final BooksControllerConfiguration                      config;
  private final File                                              data_directory;
  private final DownloaderType                                    downloader;
  private final ExecutorService                                   exec;
  private final OPDSFeedParserType                                feed_parser;
  private final HTTPType                                          http;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;
  private final AtomicInteger                                     task_id;
  private Map<Integer, Future<?>>                                 tasks;

  private BooksController(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final BooksControllerConfiguration in_config)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.feed_parser = NullCheck.notNull(in_feeds);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.downloader = NullCheck.notNull(in_downloader);
    this.tasks = new ConcurrentHashMap<Integer, Future<?>>();
    this.login = new AtomicReference<Pair<AccountBarcode, AccountPIN>>();
    this.books_status = BooksStatusCache.newStatusCache();
    this.data_directory = new File(this.config.getDirectory(), "data");
    this.book_database = BookDatabase.newDatabase(this.data_directory);
    this.task_id = new AtomicInteger(0);
  }

  @Override public void accountGetCachedLoginDetails(
    final AccountGetCachedCredentialsListenerType listener)
  {
    final Pair<AccountBarcode, AccountPIN> p = this.login.get();
    if (p != null) {
      try {
        listener.onAccountIsLoggedIn(p.getLeft(), p.getRight());
      } catch (final Throwable x) {
        BooksController.LOG.error("listener raised exception: ", x);
      }
    } else {
      try {
        listener.onAccountIsNotLoggedIn();
      } catch (final Throwable x) {
        BooksController.LOG.error("listener raised exception: ", x);
      }
    }
  }

  @Override public boolean accountIsLoggedIn()
  {
    return this.login.get() != null;
  }

  @Override public void accountLoadBooks(
    final AccountDataLoadListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new DataLoadTask(
      this.book_database,
      this.books_status,
      this.downloader,
      listener,
      this.login));
  }

  @Override public void accountLogin(
    final AccountBarcode barcode,
    final AccountPIN pin,
    final AccountLoginListenerType listener)
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);
    NullCheck.notNull(listener);

    this.submitRunnable(new LoginTask(
      this,
      this.book_database,
      this.http,
      this.config,
      barcode,
      pin,
      listener,
      this.login));
  }

  @Override public void accountLogout(
    final AccountLogoutListenerType listener)
  {
    NullCheck.notNull(listener);

    synchronized (this) {
      this.stopAllTasks();
      this.books_status.booksStatusClearAll();
      this.downloader.downloadDestroyAll();
      this.submitRunnable(new LogoutTask(this.config, this.login, listener));
    }
  }

  @Override public void accountSync(
    final AccountSyncListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new SyncTask(
      this.config,
      this,
      this.book_database,
      this.http,
      this.feed_parser,
      this.downloader,
      listener));
  }

  @Override public void bookBorrow(
    final BookID id,
    final OPDSAcquisition acq,
    final String title,
    final BookBorrowListenerType listener)
  {
    NullCheck.notNull(id);
    NullCheck.notNull(acq);
    NullCheck.notNull(listener);

    BooksController.LOG.debug("borrow {}", id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingLoan(id));
    this.submitRunnable(new BorrowTask(
      this.book_database,
      this.books_status,
      this.downloader,
      id,
      acq,
      title,
      listener));
  }

  @Override public void bookDeleteData(
    final BookID id)
  {
    NullCheck.notNull(id);

    BooksController.LOG.debug("delete: {}", id);
    this.submitRunnable(new DeleteBookDataTask(
      this.books_status,
      this.book_database,
      id));
  }

  @Override public void bookDownloadAcknowledge(
    final BookID id)
  {
    final OptionType<BookStatusType> s_opt =
      this.books_status.booksStatusGet(id);
    if (s_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) s_opt;
      final BookStatusType status = some.get();

      if (status instanceof BookStatusDownloadingType) {
        final BookStatusDownloadingType downloading =
          (BookStatusDownloadingType) status;
        final DownloadSnapshot dsnap = downloading.getDownloadSnapshot();
        this.downloader.downloadAcknowledge(dsnap.statusGetID());
        this.books_status.booksStatusUpdate(new BookStatusLoaned(id));
      }
    }
  }

  @Override public void bookDownloadCancel(
    final BookID id)
  {
    BooksController.LOG.debug("download cancel {}", id);

    final OptionType<BookStatusType> s_opt =
      this.books_status.booksStatusGet(id);

    if (s_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) s_opt;
      final BookStatusType status = some.get();

      BooksController.LOG.debug("download cancel {}: status: {}", id, status);
      status
        .matchBookStatus(new BookStatusMatcherType<Unit, UnreachableCodeException>() {
          @Override public Unit onBookStatusLoanedType(
            final BookStatusLoanedType loaned)
          {
            return loaned
              .matchBookLoanedStatus(new BookStatusLoanedMatcherType<Unit, UnreachableCodeException>() {
                @Override public Unit onBookStatusDownloaded(
                  final BookStatusDownloaded d)
                {
                  return Unit.unit();
                }

                @Override public Unit onBookStatusDownloading(
                  final BookStatusDownloadingType o)
                {
                  final DownloadSnapshot snap = o.getDownloadSnapshot();
                  final long did = snap.statusGetID();
                  BooksController.this.downloader.downloadCancel(did);
                  BooksController.this.downloader.downloadAcknowledge(did);
                  return Unit.unit();
                }

                @Override public Unit onBookStatusLoaned(
                  final BookStatusLoaned o)
                {
                  return Unit.unit();
                }

                @Override public Unit onBookStatusRequestingDownload(
                  final BookStatusRequestingDownload d)
                {
                  return Unit.unit();
                }
              });
          }

          @Override public Unit onBookStatusRequestingLoan(
            final BookStatusRequestingLoan s)
          {
            return Unit.unit();
          }
        });
    } else {
      BooksController.LOG.debug("download cancel {}: no known download", id);
    }
  }

  @Override public void booksGetFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final BookFeedListenerType in_listener)
  {
    NullCheck.notNull(in_uri);
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_updated);
    NullCheck.notNull(in_title);
    NullCheck.notNull(in_listener);

    this.submitRunnable(new FeedTask(
      this.book_database,
      in_uri,
      in_id,
      in_updated,
      in_title,
      in_listener));
  }

  @Override public synchronized void booksObservableAddObserver(
    final Observer o)
  {
    this.books_status.booksObservableAddObserver(o);
  }

  @Override public synchronized void booksObservableDeleteAllObservers()
  {
    this.books_status.booksObservableDeleteAllObservers();
  }

  @Override public synchronized void booksObservableDeleteObserver(
    final Observer o)
  {
    this.books_status.booksObservableDeleteObserver(o);
  }

  @Override public void booksObservableNotify(
    final BookID id)
  {
    this.books_status.booksObservableNotify(id);
  }

  @Override public OptionType<BookSnapshot> booksSnapshotGet(
    final BookID id)
  {
    return this.books_status.booksSnapshotGet(id);
  }

  @Override public void booksSnapshotUpdate(
    final BookID id,
    final BookSnapshot snap)
  {
    this.books_status.booksSnapshotUpdate(id, snap);
  }

  @Override public void booksStatusClearAll()
  {
    this.books_status.booksStatusClearAll();
  }

  @Override public OptionType<BookStatusType> booksStatusGet(
    final BookID id)
  {
    return this.books_status.booksStatusGet(id);
  }

  @Override public void booksStatusUpdate(
    final BookStatusType s)
  {
    this.books_status.booksStatusUpdate(s);
  }

  @Override public void booksStatusUpdateIfMoreImportant(
    final BookStatusType s)
  {
    this.books_status.booksStatusUpdateIfMoreImportant(s);
  }

  @Override public void bookUpdateMetadata(
    final BookID id,
    final OPDSAcquisitionFeedEntry e)
  {
    NullCheck.notNull(id);
    NullCheck.notNull(e);

    BooksController.LOG.debug("update metadata {}: {}", id, e);
    this.submitRunnable(new UpdateMetadataTask(
      this.http,
      this.book_database,
      id,
      e));
  }

  private void stopAllTasks()
  {
    synchronized (this) {
      final Map<Integer, Future<?>> t_old = this.tasks;
      this.tasks = new ConcurrentHashMap<Integer, Future<?>>();

      final Iterator<Future<?>> iter = t_old.values().iterator();
      while (iter.hasNext()) {
        try {
          final Future<?> f = iter.next();
          f.cancel(true);
          iter.remove();
        } catch (final Throwable x) {
          // Ignore
        }
      }
    }
  }

  private void submitRunnable(
    final Runnable r)
  {
    synchronized (this) {
      final int id = Integer.valueOf(this.task_id.incrementAndGet());
      final Runnable rb = new Runnable() {
        @Override public void run()
        {
          try {
            r.run();
          } finally {
            BooksController.this.tasks.remove(id);
          }
        }
      };
      this.tasks.put(id, this.exec.submit(rb));
    }
  }

}
