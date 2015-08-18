package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultToException;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

/**
 * The default implementation of the {@link BooksType} interface.
 */

public final class BooksController extends Observable implements BooksType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksController.class));
  }

  private final BookDatabaseType                                  book_database;
  private final BooksStatusCacheType                              books_status;
  private final BooksControllerConfiguration                      config;
  private final File
                                                                  data_directory;
  private final DownloaderType                                    downloader;
  private final ConcurrentHashMap<BookID, DownloadType>           downloads;
  private final ExecutorService                                   exec;
  private final FeedLoaderType                                    feed_loader;
  private final OPDSFeedParserType                                feed_parser;
  private final HTTPType                                          http;
  private final AtomicReference<Pair<AccountBarcode, AccountPIN>> login;
  private final AtomicInteger                                     task_id;
  private final OptionType<AdobeAdeptExecutorType>                adobe_drm;
  private       Map<Integer, Future<?>>                           tasks;

  private BooksController(
    final ExecutorService in_exec,
    final FeedLoaderType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final BooksControllerConfiguration in_config,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.feed_loader = NullCheck.notNull(in_feeds);
    this.http = NullCheck.notNull(in_http);
    this.downloader = NullCheck.notNull(in_downloader);
    this.config = NullCheck.notNull(in_config);
    this.tasks = new ConcurrentHashMap<Integer, Future<?>>(32);
    this.login = new AtomicReference<Pair<AccountBarcode, AccountPIN>>();
    this.downloads = new ConcurrentHashMap<BookID, DownloadType>(32);
    this.books_status = BooksStatusCache.newStatusCache();
    this.data_directory = new File(this.config.getDirectory(), "data");
    this.book_database = BookDatabase.newDatabase(
      in_json_serializer, in_json_parser, this.data_directory);
    this.task_id = new AtomicInteger(0);
    this.feed_parser = this.feed_loader.getOPDSFeedParser();
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
  }

  static OptionType<File> makeCover(
    final HTTPType http,
    final OptionType<URI> cover_opt)
    throws IOException
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
    throws IOException
  {
    BooksController.LOG.debug("fetching cover {}", cover_uri);

    final OptionType<HTTPAuthType> no_auth = Option.none();
    final HTTPResultOKType<InputStream> r =
      http.get(no_auth, cover_uri, 0L).matchResult(
        new HTTPResultToException<InputStream>(cover_uri));

    try {
      final FileOutputStream fs = new FileOutputStream(cover_file_tmp);
      try {
        final InputStream in = NullCheck.notNull(r.getValue());
        try {
          final byte[] buffer = new byte[8192];
          while (true) {
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

    BooksController.LOG.debug("fetched cover {}", cover_uri);
  }

  /**
   * Construct a new books controller.
   *
   * @param in_exec            An executor
   * @param in_feeds           An asynchronous feed loader
   * @param in_http            An HTTP interface
   * @param in_downloader      A downloader
   * @param in_json_serializer A JSON serializer
   * @param in_json_parser     A JSON parser
   * @param in_config          The controller configuration
   * @param in_adobe_drm       An Adobe DRM interface, if supported
   *
   * @return A new books controller
   */

  public static BooksType newBooks(
    final ExecutorService in_exec,
    final FeedLoaderType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final BooksControllerConfiguration in_config,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm)
  {
    return new BooksController(
      in_exec,
      in_feeds,
      in_http,
      in_downloader,
      in_json_serializer,
      in_json_parser,
      in_config,
      in_adobe_drm);
  }

  /**
   * Convenience function to update a given book database entry and download a
   * cover.
   *
   * @param e             The acquisition feed entry
   * @param book_database The book database
   * @param books_status  The book status cache
   * @param http          An HTTP implementation
   *
   * @throws IOException On I/O errors
   */

  static void syncFeedEntry(
    final OPDSAcquisitionFeedEntry e,
    final BookDatabaseType book_database,
    final BooksStatusCacheType books_status,
    final HTTPType http)
    throws IOException
  {
    final BookID book_id = BookID.newIDFromEntry(e);

    BooksController.LOG.debug("book {}: synchronizing book entry", book_id);
    final BookDatabaseEntryType book_dir =
      book_database.getBookDatabaseEntry(book_id);
    book_dir.create();
    book_dir.setData(e);

    BooksController.LOG.debug("book {}: fetching cover", book_id);
    final OptionType<File> cover =
      BooksController.makeCover(http, e.getCover());
    book_dir.setCover(cover);

    BooksController.LOG.debug("book {}: getting snapshot", book_id);
    final BookSnapshot snap = book_dir.getSnapshot();
    BooksController.LOG.debug("book {}: determining status", book_id);
    final BookStatusType status = BookStatus.fromSnapshot(book_id, snap);

    BooksController.LOG.debug("book {}: updating status", book_id);
    books_status.booksStatusUpdateIfMoreImportant(status);
    BooksController.LOG.debug("book {}: updating snapshot", book_id);
    books_status.booksSnapshotUpdate(book_id, snap);

    BooksController.LOG.debug(
      "book {}: finished synchronizing book entry", book_id);
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
    this.submitRunnable(
      new BooksControllerDataLoadTask(
        this.book_database, this.books_status, listener, this.login));
  }

  @Override public void accountLogin(
    final AccountBarcode barcode,
    final AccountPIN pin,
    final AccountLoginListenerType listener)
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);
    NullCheck.notNull(listener);

    this.submitRunnable(
      new BooksControllerLoginTask(
        this,
        this.book_database,
        this.http,
        this.config,
        this.adobe_drm,
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
      this.submitRunnable(
        new BooksControllerLogoutTask(
          this.config, this.adobe_drm, this.login, listener));
    }
  }

  @Override public void accountSync(
    final AccountSyncListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(
      new BooksControllerSyncTask(
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
    final OPDSAcquisitionFeedEntry eo,
    final BookBorrowListenerType listener)
  {
    NullCheck.notNull(id);
    NullCheck.notNull(acq);
    NullCheck.notNull(eo);
    NullCheck.notNull(listener);

    BooksController.LOG.debug("borrow {}", id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingLoan(id));
    this.submitRunnable(
      new BooksControllerBorrowTask(
        this.book_database,
        this.books_status,
        this.downloader,
        this.http,
        this.downloads,
        id,
        acq,
        eo,
        listener,
        this.feed_loader,
        this.adobe_drm));
  }

  @Override public void bookDeleteData(
    final BookID id)
  {
    NullCheck.notNull(id);

    BooksController.LOG.debug("delete: {}", id);
    this.submitRunnable(
      new BooksControllerDeleteBookDataTask(
        this.books_status, this.book_database, id));
  }

  @Override public void bookDownloadAcknowledge(
    final BookID id)
  {
    BooksController.LOG.debug("acknowledging download of book {}", id);

    final OptionType<BookStatusType> status_opt =
      this.books_status.booksStatusGet(id);
    if (status_opt.isSome()) {
      final Some<BookStatusType> status_some =
        (Some<BookStatusType>) status_opt;
      final BookStatusType status = status_some.get();
      BooksController.LOG.debug(
        "status of book {} is currently {}", id, status);

      if (status instanceof BookStatusDownloadFailed) {
        final OptionType<BookSnapshot> snap_opt =
          this.books_status.booksSnapshotGet(id);
        if (snap_opt.isSome()) {
          final Some<BookSnapshot> snap_some = (Some<BookSnapshot>) snap_opt;
          final BookSnapshot snap = snap_some.get();
          this.books_status.booksStatusUpdate(
            BookStatus.fromSnapshot(
              id, snap));
        } else {

          /**
           * A snapshot *must* exist for a book that has had a download
           * attempt.
           */

          throw new UnreachableCodeException();
        }
      }
    } else {
      BooksController.LOG.debug("status of book {} unavailable", id);
    }
  }

  @Override public void bookDownloadCancel(
    final BookID id)
  {
    BooksController.LOG.debug("download cancel {}", id);

    final DownloadType d = this.downloads.get(id);
    if (d != null) {
      BooksController.LOG.debug("cancelling download {}", d);
      d.cancel();
      this.downloads.remove(id);
    }
  }

  @Override public void booksGetFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final FeedFacetPseudo.FacetType in_facet_active,
    final String in_facet_group,
    final FeedFacetPseudoTitleProviderType in_facet_titles,
    final OptionType<String> in_search,
    final BooksFeedSelection in_selection,
    final BookFeedListenerType in_listener)
  {
    NullCheck.notNull(in_uri);
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_updated);
    NullCheck.notNull(in_title);
    NullCheck.notNull(in_facet_active);
    NullCheck.notNull(in_facet_group);
    NullCheck.notNull(in_facet_titles);
    NullCheck.notNull(in_search);
    NullCheck.notNull(in_selection);
    NullCheck.notNull(in_listener);

    this.submitRunnable(
      new BooksControllerFeedTask(
        this.book_database,
        in_uri,
        in_id,
        in_updated,
        in_title,
        in_facet_active,
        in_facet_group,
        in_facet_titles,
        in_search,
        in_selection,
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
    this.submitRunnable(
      new BooksControllerUpdateMetadataTask(
        this.http, this.book_database, id, e));
  }

  private void stopAllTasks()
  {
    synchronized (this) {
      final Map<Integer, Future<?>> t_old = this.tasks;
      this.tasks = new ConcurrentHashMap<Integer, Future<?>>(32);

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
      final int id = this.task_id.incrementAndGet();
      final Runnable rb = new Runnable()
      {
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
