package org.nypl.simplified.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.catalog.CatalogBookCoverGenerator;
import org.nypl.simplified.app.reader.ReaderBookmarks;
import org.nypl.simplified.app.reader.ReaderBookmarksType;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMapType;
import org.nypl.simplified.app.reader.ReaderHTTPServer;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.app.reader.ReaderSettings;
import org.nypl.simplified.app.reader.ReaderSettingsType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfiguration;
import org.nypl.simplified.books.core.BooksControllerConfigurationBuilderType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedLoader;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransport;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.nypl.simplified.opds.core.OPDSSearchParser;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.nypl.simplified.tenprint.TenPrintGenerator;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global application state.
 */

@SuppressWarnings({ "boxing", "synthetic-access" })
public final class Simplified extends Application
{
  private static final              Logger     LOG;
  private static volatile @Nullable Simplified INSTANCE;

  static {
    LOG = LogUtilities.getLog(Simplified.class);
  }

  private @Nullable CatalogAppServices app_services;
  private @Nullable ReaderAppServices  reader_services;

  /**
   * Construct the application.
   */

  public Simplified()
  {

  }

  private static Simplified checkInitialized()
  {
    final Simplified i = Simplified.INSTANCE;
    if (i == null) {
      throw new IllegalStateException("Application is not yet initialized");
    }
    return i;
  }

  /**
   * @return The application services provided to the Catalog.
   */

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

    if (Environment.MEDIA_MOUNTED.equals(
      Environment.getExternalStorageState())) {

      Simplified.LOG.debug("trying external storage");
      if (Environment.isExternalStorageRemovable() == false) {
        final File r = context.getExternalFilesDir(null);
        Simplified.LOG.debug(
          "external storage is not removable, using it ({})", r);
        Assertions.checkPrecondition(
          r.isDirectory(), "Data directory {} is a directory", r);
        return NullCheck.notNull(r);
      }
    }

    /**
     * Otherwise, use internal storage.
     */

    final File r = context.getFilesDir();
    Simplified.LOG.debug(
      "no non-removable external storage, using internal storage ({})");
    Assertions.checkPrecondition(
      r.isDirectory(), "Data directory {} is a directory", r);
    return NullCheck.notNull(r);
  }

  /**
   * @return The application services provided to the Reader.
   */

  public static SimplifiedReaderAppServicesType getReaderAppServices()
  {
    final Simplified i = Simplified.checkInitialized();
    return i.getActualReaderAppServices();
  }

  private static FeedLoaderType makeFeedLoader(
    final ExecutorService exec,
    final OPDSSearchParserType s,
    final OPDSFeedParserType p)
  {
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    return FeedLoader.newFeedLoader(exec, p, t, s);
  }

  private static ExecutorService namedThreadPool(
    final int count,
    final String base,
    final int priority)
  {
    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ThreadFactory named = new ThreadFactory()
    {
      private int id;

      @Override public Thread newThread(
        final @Nullable Runnable r)
      {
        /**
         * Apparently, it's necessary to use {@link android.os.Process} to set
         * the thread priority, rather than the standard Java thread
         * functions.
         */

        final Thread t = tf.newThread(
          new Runnable()
          {
            @Override public void run()
            {
              android.os.Process.setThreadPriority(priority);
              NullCheck.notNull(r).run();
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

  private synchronized SimplifiedCatalogAppServicesType getActualAppServices()
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
    Simplified.LOG.debug("starting app: pid {}", android.os.Process.myPid());
    Simplified.INSTANCE = this;
  }

  private static final class CatalogAppServices implements
    SimplifiedCatalogAppServicesType,
    AccountDataLoadListenerType,
    AccountSyncListenerType
  {
    private static final Logger LOG_CA;

    static {
      LOG_CA = LogUtilities.getLog(CatalogAppServices.class);
    }

    private final BooksType                 books;
    private final Context                   context;
    private final CatalogBookCoverGenerator cover_generator;
    private final BookCoverProviderType     cover_provider;
    private final ExecutorService           exec_books;
    private final ExecutorService           exec_catalog_feeds;
    private final ExecutorService           exec_covers;
    private final ExecutorService           exec_downloader;
    private final URI                       feed_initial_uri;
    private final FeedLoaderType            feed_loader;
    private final HTTPType                  http;
    private final ScreenSizeControllerType  screen;
    private final AtomicBoolean             synced;
    private final DownloaderType            downloader;

    public CatalogAppServices(
      final Context in_context,
      final Resources rr)
    {
      NullCheck.notNull(rr);

      this.context = NullCheck.notNull(in_context);
      this.screen = new ScreenSizeController(rr);
      this.exec_catalog_feeds =
        Simplified.namedThreadPool(1, "catalog-feed", 19);
      this.exec_covers = Simplified.namedThreadPool(2, "cover", 19);
      this.exec_downloader = Simplified.namedThreadPool(4, "downloader", 19);
      this.exec_books = Simplified.namedThreadPool(1, "books", 19);

      /**
       * Catalog URIs.
       */

      this.feed_initial_uri =
        NullCheck.notNull(URI.create(rr.getString(R.string.catalog_start_uri)));

      final OPDSAcquisitionFeedEntryParserType in_entry_parser =
        OPDSAcquisitionFeedEntryParser.newParser();

      final OPDSJSONSerializerType in_json_serializer =
        OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

      final OPDSFeedParserType p = OPDSFeedParser.newParser(in_entry_parser);
      final OPDSSearchParserType s = OPDSSearchParser.newParser();
      this.feed_loader =
        Simplified.makeFeedLoader(this.exec_catalog_feeds, s, p);

      /**
       * Book management.
       */

      final File base_dir = Simplified.getDiskDataDir(in_context);
      final File downloads_dir = new File(base_dir, "downloads");
      final File books_dir = new File(base_dir, "books");

      /**
       * Make sure the required directories exist. There is no sane way to
       * recover if they cannot be created!
       */

      try {
        DirectoryUtilities.directoryCreate(downloads_dir);
        DirectoryUtilities.directoryCreate(books_dir);
      } catch (final IOException e) {
        Simplified.LOG.error(
          "could not create directories: {}", e.getMessage(), e);
        throw new IllegalStateException(e);
      }

      CatalogAppServices.LOG_CA.debug("base:      {}", base_dir);
      CatalogAppServices.LOG_CA.debug("downloads: {}", downloads_dir);
      CatalogAppServices.LOG_CA.debug("books:     {}", books_dir);

      this.http = HTTP.newHTTP();
      this.downloader = DownloaderHTTP.newDownloader(
        this.exec_books, downloads_dir, this.http);

      final BooksControllerConfigurationBuilderType bcb =
        BooksControllerConfiguration.newBuilder(books_dir);
      final BooksControllerConfiguration books_config = bcb.build();

      this.books = BooksController.newBooks(
        this.exec_books,
        this.feed_loader,
        this.http,
        this.downloader,
        in_json_serializer,
        in_json_parser,
        books_config);

      /**
       * Configure cover provider.
       */

      final TenPrintGeneratorType ten_print = TenPrintGenerator.newGenerator();
      this.cover_generator = new CatalogBookCoverGenerator(ten_print);
      this.cover_provider = BookCoverProvider.newCoverProvider(
        in_context, this.books, this.cover_generator, this.exec_covers);

      this.synced = new AtomicBoolean(false);
    }

    @Override public BooksType getBooks()
    {
      return this.books;
    }

    @Override public BookCoverProviderType getCoverProvider()
    {
      return this.cover_provider;
    }

    @Override public URI getFeedInitialURI()
    {
      return this.feed_initial_uri;
    }

    @Override public FeedLoaderType getFeedLoader()
    {
      return this.feed_loader;
    }

    @Override public boolean isNetworkAvailable()
    {
      final NetworkInfo info =
        ((ConnectivityManager) this.context.getSystemService(
          Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

      if (info == null) {
        return false;
      }

      return info.isConnected();
    }

    @Override public void onAccountDataBookLoadFailed(
      final BookID id,
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String.format("failed to load books: %s", message));
      LogUtilities.errorWithOptionalException(
        CatalogAppServices.LOG_CA, s, error);
    }

    @Override public void onAccountDataBookLoadFinished()
    {
      CatalogAppServices.LOG_CA.debug(
        "finished loading books, syncing " + "account");
      final BooksType b = NullCheck.notNull(this.books);
      b.accountSync(this);
    }

    @Override public void onAccountDataBookLoadSucceeded(
      final BookID book,
      final BookSnapshot snap)
    {
      CatalogAppServices.LOG_CA.debug("loaded book: {}", book);
    }

    @Override public void onAccountDataLoadFailedImmediately(
      final Throwable error)
    {
      CatalogAppServices.LOG_CA.error("failed to load books: ", error);
    }

    @Override public void onAccountSyncAuthenticationFailure(
      final String message)
    {
      CatalogAppServices.LOG_CA.debug(
        "failed to sync account due to authentication failure: {}", message);
    }

    @Override public void onAccountSyncBook(
      final BookID book)
    {
      CatalogAppServices.LOG_CA.debug("synced book: {}", book);
    }

    @Override public void onAccountSyncFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String.format("failed to sync account: %s", message));
      LogUtilities.errorWithOptionalException(
        CatalogAppServices.LOG_CA, s, error);
    }

    @Override public void onAccountSyncSuccess()
    {
      CatalogAppServices.LOG_CA.debug("synced account");
    }

    @Override public void onAccountUnavailable()
    {
      CatalogAppServices.LOG_CA.debug("not logged in, not loading books");
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      return this.screen.screenDPToPixels(dp);
    }

    @Override public double screenGetDPI()
    {
      return this.screen.screenGetDPI();
    }

    @Override public int screenGetHeightPixels()
    {
      return this.screen.screenGetHeightPixels();
    }

    @Override public int screenGetWidthPixels()
    {
      return this.screen.screenGetWidthPixels();
    }

    @Override public boolean screenIsLarge()
    {
      return this.screen.screenIsLarge();
    }

    @Override public void syncInitial()
    {
      if (this.synced.compareAndSet(false, true)) {
        CatalogAppServices.LOG_CA.debug("performing initial sync");
        this.books.accountLoadBooks(this);
      } else {
        CatalogAppServices.LOG_CA.debug(
          "initial sync already attempted, not syncing again");
      }
    }
  }

  private static final class ReaderAppServices
    implements SimplifiedReaderAppServicesType
  {
    private final ReaderBookmarksType         bookmarks;
    private final ExecutorService             epub_exec;
    private final ReaderReadiumEPUBLoaderType epub_loader;
    private final ExecutorService             http_executor;
    private final ReaderHTTPServerType        httpd;
    private final ReaderHTTPMimeMapType       mime;
    private final ScreenSizeControllerType    screen;
    private final ReaderSettingsType          settings;

    public ReaderAppServices(
      final Context context,
      final Resources rr)
    {
      this.screen = new ScreenSizeController(rr);

      this.mime = ReaderHTTPMimeMap.newMap("application/octet-stream");
      this.http_executor = Simplified.namedThreadPool(1, "httpd", 19);
      this.httpd =
        ReaderHTTPServer.newServer(this.http_executor, this.mime, 8080);

      this.epub_exec = Simplified.namedThreadPool(1, "epub", 19);
      this.epub_loader = ReaderReadiumEPUBLoader.newLoader(this.epub_exec);

      this.settings = ReaderSettings.openSettings(context);
      this.bookmarks = ReaderBookmarks.openBookmarks(context);
    }

    @Override public ReaderBookmarksType getBookmarks()
    {
      return this.bookmarks;
    }

    @Override public ReaderReadiumEPUBLoaderType getEPUBLoader()
    {
      return this.epub_loader;
    }

    @Override public ReaderHTTPServerType getHTTPServer()
    {
      return this.httpd;
    }

    @Override public ReaderSettingsType getSettings()
    {
      return this.settings;
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      return this.screen.screenDPToPixels(dp);
    }

    @Override public double screenGetDPI()
    {
      return this.screen.screenGetDPI();
    }

    @Override public int screenGetHeightPixels()
    {
      return this.screen.screenGetHeightPixels();
    }

    @Override public int screenGetWidthPixels()
    {
      return this.screen.screenGetWidthPixels();
    }

    @Override public boolean screenIsLarge()
    {
      return this.screen.screenIsLarge();
    }
  }

  private static final class ScreenSizeController
    implements ScreenSizeControllerType
  {
    private final Resources resources;

    public ScreenSizeController(
      final Resources rr)
    {
      this.resources = NullCheck.notNull(rr);

      final DisplayMetrics dm = this.resources.getDisplayMetrics();
      final float dp_height = dm.heightPixels / dm.density;
      final float dp_width = dm.widthPixels / dm.density;
      CatalogAppServices.LOG_CA.debug("screen ({} x {})", dp_width, dp_height);
      CatalogAppServices.LOG_CA.debug(
        "screen ({} x {})", dm.widthPixels, dm.heightPixels);
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
      large |= (s & Configuration.SCREENLAYOUT_SIZE_LARGE)
               == Configuration.SCREENLAYOUT_SIZE_LARGE;
      large |= (s & Configuration.SCREENLAYOUT_SIZE_XLARGE)
               == Configuration.SCREENLAYOUT_SIZE_XLARGE;

      if (rr.getBoolean(R.bool.debug_override_large_screen)) {
        if (large == false) {
          Simplified.LOG.debug(
            "screen size overridden to be large by "
            + "debug_override_large_screen");
          return true;
        }
      }

      if (rr.getBoolean(R.bool.debug_override_small_screen)) {
        if (large == true) {
          Simplified.LOG.debug(
            "screen size overridden to be small by "
            + "debug_override_small_screen");
          return false;
        }
      }

      return large;
    }
  }
}
