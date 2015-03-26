package org.nypl.simplified.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.Book;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.Books;
import org.nypl.simplified.books.core.BooksConfiguration;
import org.nypl.simplified.books.core.BooksConfigurationBuilderType;
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

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Global application state.
 */

@SuppressWarnings("boxing") public final class Simplified extends Application implements
  ScreenSizeControllerType,
  MemoryControllerType
{
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

  public static Simplified get()
  {
    return Simplified.checkInitialized();
  }

  private static File getDiskCacheDir(
    final Context context)
  {
    /**
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for caching.
     */

    if (Environment.MEDIA_MOUNTED.equals(Environment
      .getExternalStorageState())) {
      if (Environment.isExternalStorageRemovable() == false) {
        return NullCheck.notNull(context.getExternalCacheDir());
      }
    }

    /**
     * Otherwise, use internal storage.
     */

    return NullCheck.notNull(context.getCacheDir());
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

  private static CatalogAcquisitionCoverCacheType makeCoverCache(
    final ListeningExecutorService list_exec,
    final MemoryControllerType mem,
    final Context ctx,
    final Resources rr)
    throws IOException
  {
    final PartialFunctionType<URI, InputStream, IOException> it =
      BitmapTransport.get();
    final File cd = new File(Simplified.getDiskCacheDir(ctx), "covers");
    Log.d(Simplified.TAG, "cover cache: " + cd);
    cd.mkdirs();
    Preconditions.checkArgument(cd.exists());
    Preconditions.checkArgument(cd.isDirectory());

    final int disk_thumbnail_size =
      Simplified.megabytesToBytes(rr
        .getInteger(R.integer.image_cover_disk_cache_megabytes));

    final BitmapCacheScalingDiskType bcf =
      BitmapCacheScalingDisk.newCache(it, mem, cd, disk_thumbnail_size);
    final CatalogAcquisitionCoverGenerator cgen =
      new CatalogAcquisitionCoverGenerator();
    return CatalogAcquisitionImageCache.newCoverCache(list_exec, cgen, bcf);
  }

  private static OPDSFeedLoaderType makeFeedLoader(
    final ExecutorService exec,
    final OPDSFeedParserType p)
  {
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    final OPDSFeedLoaderType flx = OPDSFeedLoader.newLoader(exec, p, t);
    return CachingFeedLoader.newLoader(flx);
  }

  private static CatalogAcquisitionThumbnailCacheType makeThumbnailCache(
    final ListeningExecutorService list_exec,
    final MemoryControllerType mem,
    final Context ctx,
    final Resources rr)
    throws IOException
  {
    final PartialFunctionType<URI, InputStream, IOException> it =
      BitmapTransport.get();
    final File cd = new File(Simplified.getDiskCacheDir(ctx), "thumbnails");
    Log.d(Simplified.TAG, "thumbnail cache: " + cd);
    cd.mkdirs();
    Preconditions.checkArgument(cd.exists());
    Preconditions.checkArgument(cd.isDirectory());

    final int disk_thumbnail_size =
      Simplified.megabytesToBytes(rr
        .getInteger(R.integer.image_thumbnail_disk_cache_megabytes));
    final int mem_size =
      Simplified.megabytesToBytes(rr
        .getInteger(R.integer.image_memory_cache_megabytes));

    final BitmapCacheScalingDiskType bcf =
      BitmapCacheScalingDisk.newCache(it, mem, cd, disk_thumbnail_size);

    BitmapCacheScalingType bc;
    if (mem.memoryIsSmall()) {
      bc = bcf;
    } else {
      Log.d(
        Simplified.TAG,
        "non-small heap detected, using extra in-memory bitmap cache");
      bc = BitmapCacheScalingMemoryProxy.newCache(bcf, mem_size);
    }

    final CatalogAcquisitionCoverGenerator cgen =
      new CatalogAcquisitionCoverGenerator();
    return CatalogAcquisitionImageCache
      .newThumbnailCache(list_exec, cgen, bc);
  }

  private static int megabytesToBytes(
    final int m)
  {
    return (int) (m * Math.pow(10, 7));
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

  private @Nullable BooksType                            books;
  private @Nullable CatalogAcquisitionCoverCacheType     catalog_acquisition_cover_loader;
  private @Nullable CatalogAcquisitionThumbnailCacheType catalog_thumbnail_loader;
  private @Nullable ListeningExecutorService             catalog_exec_decor;
  private @Nullable ExecutorService                      catalog_executor;
  private @Nullable URI                                  feed_initial_uri;
  private @Nullable OPDSFeedLoaderType                   feed_loader;
  private @Nullable HTTPType                             http;
  private int                                            memory;
  private boolean                                        memory_small;

  public BooksType getBooks()
  {
    return NullCheck.notNull(this.books);
  }

  public CatalogAcquisitionCoverCacheType getCatalogAcquisitionCoverLoader()
  {
    return NullCheck.notNull(this.catalog_acquisition_cover_loader);
  }

  public CatalogAcquisitionThumbnailCacheType getCatalogThumbnailLoader()
  {
    return NullCheck.notNull(this.catalog_thumbnail_loader);
  }

  public URI getFeedInitialURI()
  {
    return NullCheck.notNull(this.feed_initial_uri);
  }

  public OPDSFeedLoaderType getFeedLoader()
  {
    return NullCheck.notNull(this.feed_loader);
  }

  public ListeningExecutorService getCatalogListeningExecutorService()
  {
    return NullCheck.notNull(this.catalog_exec_decor);
  }

  @Override public int memoryGetSize()
  {
    return this.memory;
  }

  @Override public boolean memoryIsSmall()
  {
    return this.memory_small;
  }

  @Override public void onCreate()
  {
    try {
      super.onCreate();

      Log.d(Simplified.TAG, "initializing application context");

      {
        final int cores = Runtime.getRuntime().availableProcessors();
        Log.d(Simplified.TAG, String.format("%d cores available", cores));
      }

      final ExecutorService in_catalog_executor =
        Simplified.namedThreadPool(3, "catalog");
      final ExecutorService in_books_executor =
        Simplified.namedThreadPool(1, "books");

      final ListeningExecutorService le =
        NullCheck.notNull(MoreExecutors
          .listeningDecorator(in_catalog_executor));
      final Resources rr = NullCheck.notNull(this.getResources());

      /**
       * Determine memory conditions.
       */

      {
        final ActivityManager am =
          (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        this.memory = am.getMemoryClass();
        this.memory_small = this.memory <= 32;
        Log.d(Simplified.TAG, String.format(
          "available memory: %dmb (small: %s)",
          this.memory,
          this.memory_small));
      }

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
        URI.create(rr.getString(R.string.catalog_start_uri));

      this.catalog_executor = in_catalog_executor;
      this.catalog_exec_decor = le;

      final OPDSFeedParserType p = OPDSFeedParser.newParser();
      this.feed_loader = Simplified.makeFeedLoader(in_catalog_executor, p);
      this.catalog_thumbnail_loader =
        Simplified.makeThumbnailCache(le, this, this, rr);
      this.catalog_acquisition_cover_loader =
        Simplified.makeCoverCache(le, this, this, rr);

      /**
       * Book management.
       */

      final File data_dir = Simplified.getDiskDataDir(this);
      final File downloads_dir = new File(data_dir, "downloads");
      final File books_dir = new File(data_dir, "books");

      Log.d(Simplified.TAG, "data: " + data_dir);
      Log.d(Simplified.TAG, "downloads: " + downloads_dir);
      Log.d(Simplified.TAG, "books: " + books_dir);

      final DownloaderConfigurationBuilderType dcb =
        DownloaderConfiguration.newBuilder(downloads_dir);
      final DownloaderConfiguration downloader_config = dcb.build();

      final HTTPType h = HTTP.newHTTP();
      final DownloaderType d =
        Downloader.newDownloader(in_books_executor, h, downloader_config);

      final BooksConfigurationBuilderType bcb =
        BooksConfiguration.newBuilder(books_dir);
      final BooksConfiguration books_config = bcb.build();

      final BooksType b =
        Books.newBooks(in_books_executor, p, h, d, books_config);

      b.accountLoadBooks(new AccountDataLoadListenerType() {
        @Override public void onAccountDataBookLoadFailed(
          final BookID id,
          final OptionType<Throwable> error,
          final String message)
        {
          final String s =
            NullCheck.notNull(String.format(
              "failed to load books: %s",
              message));
          if (error.isSome()) {
            final Some<Throwable> some = (Some<Throwable>) error;
            Log.e(Simplified.TAG_BOOKS, s, some.get());
          } else {
            Log.e(Simplified.TAG_BOOKS, s);
          }
        }

        @Override public void onAccountDataBookLoadSucceeded(
          final Book book)
        {
          Log.d(
            Simplified.TAG_BOOKS,
            String.format("loaded book: %s", book.getID()));
        }

        @Override public void onAccountUnavailable()
        {
          Log.d(Simplified.TAG_BOOKS, "not logged in, not loading books");
        }
      });

      this.http = h;
      this.books = b;

      Simplified.INSTANCE = this;
    } catch (final NotFoundException e) {
      throw new UnreachableCodeException(e);
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override public double screenDPToPixels(
    final int dp)
  {
    final float scale = this.getResources().getDisplayMetrics().density;
    return ((dp * scale) + 0.5);
  }

  @Override public double screenGetDPI()
  {
    final DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    return metrics.densityDpi;
  }

  @Override public int screenGetHeightPixels()
  {
    final Resources rr = NullCheck.notNull(this.getResources());
    final DisplayMetrics dm = rr.getDisplayMetrics();
    return dm.heightPixels;
  }

  @Override public int screenGetWidthPixels()
  {
    final Resources rr = NullCheck.notNull(this.getResources());
    final DisplayMetrics dm = rr.getDisplayMetrics();
    return dm.widthPixels;
  }

  @Override public boolean screenIsLarge()
  {
    final Resources rr = NullCheck.notNull(this.getResources());
    final Configuration c = NullCheck.notNull(rr.getConfiguration());
    final int s = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    boolean large = false;
    large |=
      (s & Configuration.SCREENLAYOUT_SIZE_LARGE) == Configuration.SCREENLAYOUT_SIZE_LARGE;
    large |=
      (s & Configuration.SCREENLAYOUT_SIZE_XLARGE) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    return large;
  }
}
