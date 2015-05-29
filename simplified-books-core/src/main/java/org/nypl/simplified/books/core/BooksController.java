package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultToException;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializerType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The default implementation of the {@link BooksType} interface.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class BooksController extends
  Observable implements BooksType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksController.class));
  }

  static OptionType<File> makeCover(
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
        new HTTPResultToException<InputStream>(cover_uri));

    try {
      final FileOutputStream fs = new FileOutputStream(cover_file_tmp);
      try {
        final InputStream in = NullCheck.notNull(r.getValue());
        try {
          final byte[] buffer = new byte[8192];
          for (;;) {
            final int rb = in.read(buffer);
            if (rb == -1) {
              break;
            }
            fs.write(buffer, 0, rb);
          }
        } finally {
          in.close();
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
    final OPDSAcquisitionFeedEntryParserType in_parser,
    final OPDSAcquisitionFeedEntrySerializerType in_serializer,
    final BooksControllerConfiguration in_config)
  {
    return new BooksController(
      in_exec,
      in_feeds,
      in_http,
      in_downloader,
      in_parser,
      in_serializer,
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
    final OPDSAcquisitionFeedEntryParserType in_parser,
    final OPDSAcquisitionFeedEntrySerializerType in_serializer,
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
    this.book_database =
      BookDatabase.newDatabase(in_parser, in_serializer, this.data_directory);
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
    this.submitRunnable(new BooksControllerDataLoadTask(
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

    this.submitRunnable(new BooksControllerLoginTask(
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
      this.submitRunnable(new BooksControllerLogoutTask(
        this.config,
        this.login,
        listener));
    }
  }

  @Override public void accountSync(
    final AccountSyncListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new BooksControllerSyncTask(
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
    this.submitRunnable(new BooksControllerBorrowTask(
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
    this.submitRunnable(new BooksControllerDeleteBookDataTask(
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
    final FeedFacetPseudo.Type in_facet_active,
    final String in_facet_group,
    final FeedFacetPseudoTitleProviderType in_facet_titles,
    final BookFeedListenerType in_listener)
  {
    NullCheck.notNull(in_uri);
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_updated);
    NullCheck.notNull(in_title);
    NullCheck.notNull(in_facet_active);
    NullCheck.notNull(in_facet_group);
    NullCheck.notNull(in_facet_titles);
    NullCheck.notNull(in_listener);

    this.submitRunnable(new BooksControllerFeedTask(
      this.book_database,
      in_uri,
      in_id,
      in_updated,
      in_title,
      in_facet_active,
      in_facet_group,
      in_facet_titles,
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
    this.submitRunnable(new BooksControllerUpdateMetadataTask(
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

  void submitRunnable(
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
