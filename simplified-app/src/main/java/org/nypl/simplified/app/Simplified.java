package org.nypl.simplified.app;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nypl.simplified.app.catalog.CachingFeedLoader;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMapType;
import org.nypl.simplified.app.reader.ReaderHTTPServer;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfiguration;
import org.nypl.simplified.books.core.BooksControllerConfigurationBuilderType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderConfigurationBuilderType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedLoader;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransport;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * Global application state.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class Simplified extends
  Application
{
  private static final class CatalogAppServices implements
    SimplifiedCatalogAppServicesType,
    AccountDataLoadListenerType,
    AccountSyncListenerType
  {
    private final BooksType          books;
    private final ExecutorService    books_executor;
    private final ExecutorService    catalog_executor;
    private final CoverProviderType  cover_provider;
    private final DownloaderType     downloader;
    private final URI                feed_initial_uri;
    private final OPDSFeedLoaderType feed_loader;
    private final HTTPType           http;
    private final Resources          resources;
    private final AtomicBoolean      synced;

    public CatalogAppServices(
      final Context context,
      final Resources rr)
    {
      this.resources = NullCheck.notNull(rr);
      this.catalog_executor = Simplified.namedThreadPool(3, "catalog");
      this.books_executor = Simplified.namedThreadPool(1, "books");

      /**
       * Determine screen details.
       */

      {
        final DisplayMetrics dm = rr.getDisplayMetrics();
        final float dp_height = dm.heightPixels / dm.density;
        final float dp_width = dm.widthPixels / dm.density;
        Log.d(
          Simplified.TAG,
          String.format("screen (%.2fdp x %.2fdp)", dp_width, dp_height));
        Log.d(Simplified.TAG, String.format(
          "screen (%dpx x %dpx)",
          dm.widthPixels,
          dm.heightPixels));
      }

      /**
       * Catalog URIs.
       */

      this.feed_initial_uri =
        NullCheck
          .notNull(URI.create(rr.getString(R.string.catalog_start_uri)));

      final OPDSFeedParserType p = OPDSFeedParser.newParser();
      this.feed_loader = Simplified.makeFeedLoader(this.catalog_executor, p);

      /**
       * Book management.
       */

      final File data_dir = Simplified.getDiskDataDir(context);
      final File downloads_dir = new File(data_dir, "downloads");
      final File books_dir = new File(data_dir, "books");

      Log.d(Simplified.TAG, "data: " + data_dir);
      Log.d(Simplified.TAG, "downloads: " + downloads_dir);
      Log.d(Simplified.TAG, "books: " + books_dir);

      final DownloaderConfigurationBuilderType dcb =
        DownloaderConfiguration.newBuilder(downloads_dir);
      dcb.setReadSleepTime(1000);
      final DownloaderConfiguration downloader_config = dcb.build();

      this.http = HTTP.newHTTP();
      this.downloader =
        Downloader.newDownloader(
          this.books_executor,
          this.http,
          downloader_config);

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(books_dir);
      final BooksControllerConfiguration books_config = bcb.build();

      this.books =
        BooksController.newBooks(
          this.books_executor,
          p,
          this.http,
          this.downloader,
          books_config);

      /**
       * Configure cover provider.
       */

      this.cover_provider =
        CoverProvider.newCoverProvider(
          context,
          this.books,
          this.catalog_executor);

      this.synced = new AtomicBoolean(false);
    }

    @Override public BooksType getBooks()
    {
      return this.books;
    }

    @Override public CoverProviderType getCoverProvider()
    {
      return this.cover_provider;
    }

    @Override public URI getFeedInitialURI()
    {
      return this.feed_initial_uri;
    }

    @Override public OPDSFeedLoaderType getFeedLoader()
    {
      return this.feed_loader;
    }

    @Override public void onAccountDataBookLoadFailed(
      final BookID id,
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String.format("failed to load books: %s", message));
      if (error.isSome()) {
        final Some<Throwable> some = (Some<Throwable>) error;
        Log.e(Simplified.TAG_BOOKS, s, some.get());
      } else {
        Log.e(Simplified.TAG_BOOKS, s);
      }
    }

    @Override public void onAccountDataBookLoadFinished()
    {
      Log.d(Simplified.TAG_BOOKS, "finished loading books, syncing account");
      final BooksType b = NullCheck.notNull(this.books);
      b.accountSync(this);
    }

    @Override public void onAccountDataBookLoadSucceeded(
      final BookID book,
      final BookSnapshot snap)
    {
      Log.d(Simplified.TAG_BOOKS, String.format("loaded book: %s", book));
    }

    @Override public void onAccountSyncAuthenticationFailure(
      final String message)
    {
      Log.d(
        Simplified.TAG_BOOKS,
        "failed to sync account due to authentication failure: " + message);
    }

    @Override public void onAccountSyncBook(
      final BookID book)
    {
      Log.d(Simplified.TAG_BOOKS, "synced book " + book);
    }

    @Override public void onAccountSyncFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String
          .format("failed to sync account: %s", message));
      if (error.isSome()) {
        final Some<Throwable> some = (Some<Throwable>) error;
        Log.e(Simplified.TAG_BOOKS, s, some.get());
      } else {
        Log.e(Simplified.TAG_BOOKS, s);
      }
    }

    @Override public void onAccountSyncSuccess()
    {
      Log.d(Simplified.TAG_BOOKS, "synced account");
    }

    @Override public void onAccountUnavailable()
    {
      Log.d(Simplified.TAG_BOOKS, "not logged in, not loading books");
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      final float scale = this.resources.getDisplayMetrics().density;
      return ((dp * scale) + 0.5);
    }

    @Override public double screenGetDPI()
    {
      final DisplayMetrics metrics = this.resources.getDisplayMetrics();
      return metrics.densityDpi;
    }

    @Override public int screenGetHeightPixels()
    {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.heightPixels;
    }

    @Override public int screenGetWidthPixels()
    {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.widthPixels;
    }

    @Override public boolean screenIsLarge()
    {
      final Resources rr = NullCheck.notNull(this.resources);
      final Configuration c = NullCheck.notNull(rr.getConfiguration());
      final int s = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
      boolean large = false;
      large |=
        (s & Configuration.SCREENLAYOUT_SIZE_LARGE) == Configuration.SCREENLAYOUT_SIZE_LARGE;
      large |=
        (s & Configuration.SCREENLAYOUT_SIZE_XLARGE) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
      return large;
    }

    @Override public void syncInitial()
    {
      if (this.synced.compareAndSet(false, true)) {
        Log.d(Simplified.TAG, "performing initial sync");
        this.books.accountLoadBooks(this);
      } else {
        Log.d(
          Simplified.TAG,
          "initial sync already attempted, not syncing again");
      }
    }
  }

  private static final class ReaderAppServices implements
    SimplifiedReaderAppServicesType
  {
    private final ExecutorService             epub_exec;
    private final ReaderReadiumEPUBLoaderType epub_loader;
    private final ExecutorService             http_executor;
    private final ReaderHTTPServerType        httpd;
    private final ReaderHTTPMimeMapType       mime;

    public ReaderAppServices(
      final Context context,
      final Resources rr)
    {
      this.mime = ReaderHTTPMimeMap.newMap("application/octet-stream");
      this.http_executor = Simplified.namedThreadPool(1, "httpd");
      this.httpd =
        ReaderHTTPServer.newServer(this.http_executor, this.mime, 8080);

      this.epub_exec = Simplified.namedThreadPool(1, "epub");
      this.epub_loader = ReaderReadiumEPUBLoader.newLoader(this.epub_exec);
    }

    @Override public ReaderReadiumEPUBLoaderType getEPUBLoader()
    {
      return this.epub_loader;
    }

    @Override public ReaderHTTPServerType getHTTPServer()
    {
      return this.httpd;
    }
  }

  private static volatile @Nullable Simplified INSTANCE;
  private static final String                  TAG;
  private static final String                  TAG_BOOKS;

  static {
    TAG = "S";
    TAG_BOOKS = "S.B";
  }

  private static Simplified checkInitialized()
  {
    final Simplified i = Simplified.INSTANCE;
    if (i == null) {
      throw new IllegalStateException("Application is not yet initialized");
    }
    return i;
  }

  public static SimplifiedCatalogAppServicesType getCatalogAppServices()
  {
    final Simplified i = Simplified.checkInitialized();
    return i.getActualAppServices();
  }

  private static File getDiskDataDir(
    final Context context)
  {
    /**
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for data.
     */

    if (Environment.MEDIA_MOUNTED.equals(Environment
      .getExternalStorageState())) {
      if (Environment.isExternalStorageRemovable() == false) {
        return NullCheck.notNull(context.getExternalFilesDir(null));
      }
    }

    /**
     * Otherwise, use internal storage.
     */

    return NullCheck.notNull(context.getFilesDir());
  }

  public static SimplifiedReaderAppServicesType getReaderAppServices()
  {
    final Simplified i = Simplified.checkInitialized();
    return i.getActualReaderAppServices();
  }

  private static OPDSFeedLoaderType makeFeedLoader(
    final ExecutorService exec,
    final OPDSFeedParserType p)
  {
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    final OPDSFeedLoaderType flx = OPDSFeedLoader.newLoader(exec, p, t);
    return CachingFeedLoader.newLoader(flx);
  }

  private static ExecutorService namedThreadPool(
    final int count,
    final String base)
  {
    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ThreadFactory named = new ThreadFactory() {
      private int id = 0;

      @Override public Thread newThread(
        final @Nullable Runnable r)
      {
        /**
         * Apparently, it's necessary to use {@link android.os.Process} to set
         * the thread priority, rather than the standard Java thread
         * functions. All worker threads are set to the lowest priority (19).
         */

        final Thread t = tf.newThread(new Runnable() {
          @Override public void run()
          {
            assert r != null;
            android.os.Process.setThreadPriority(19);
            r.run();
          }
        });
        t.setName(String.format("simplified-%s-tasks-%d", base, this.id));
        ++this.id;
        return t;
      }
    };

    final ExecutorService pool = Executors.newFixedThreadPool(count, named);
    return NullCheck.notNull(pool);
  }

  private @Nullable CatalogAppServices app_services;

  private @Nullable ReaderAppServices  reader_services;

  private synchronized
    SimplifiedCatalogAppServicesType
    getActualAppServices()
  {
    CatalogAppServices as = this.app_services;
    if (as != null) {
      return as;
    }
    as = new CatalogAppServices(this, NullCheck.notNull(this.getResources()));
    this.app_services = as;
    return as;
  }

  private SimplifiedReaderAppServicesType getActualReaderAppServices()
  {
    ReaderAppServices as = this.reader_services;
    if (as != null) {
      return as;
    }
    as = new ReaderAppServices(this, NullCheck.notNull(this.getResources()));
    this.reader_services = as;
    return as;
  }

  @Override public void onCreate()
  {
    Log.d(
      Simplified.TAG,
      String.format("starting app: pid %d", android.os.Process.myPid()));
    Simplified.INSTANCE = this;
  }
}
