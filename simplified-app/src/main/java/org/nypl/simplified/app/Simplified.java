package org.nypl.simplified.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Global application state.
 */

public final class Simplified extends Application implements
  ScreenSizeControllerType,
  MemoryControllerType
{
  private static final String                  TAG;

  static {
    TAG = "simplified";
  }

  private static volatile @Nullable Simplified INSTANCE;

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

  private static int megabytesToBytes(
    final int m)
  {
    return (int) (m * Math.pow(10, 7));
  }

  private static ExecutorService namedThreadPool()
  {
    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ThreadFactory named = new ThreadFactory() {
      private int id = 0;

      @SuppressWarnings("boxing") @Override public Thread newThread(
        final @Nullable Runnable r)
      {
        final Thread t = tf.newThread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.setName(String.format("simplified-tasks-%d", this.id));
        ++this.id;
        return t;
      }
    };

    final int count = Runtime.getRuntime().availableProcessors();
    final ExecutorService pool = Executors.newFixedThreadPool(count, named);
    return NullCheck.notNull(pool);
  }

  private @Nullable CatalogAcquisitionImageCacheType catalog_acquisition_image_loader;
  private @Nullable ListeningExecutorService         exec_decor;
  private @Nullable ExecutorService                  executor;
  private @Nullable URI                              feed_initial_uri;
  private @Nullable OPDSFeedLoaderType               feed_loader;
  private @Nullable OPDSFeedParserType               feed_parser;
  private @Nullable OPDSFeedTransportType            feed_transport;
  private @Nullable BitmapCacheScalingType           image_loader;
  private int                                        memory;
  private boolean                                    memory_small;

  public CatalogAcquisitionImageCacheType getCatalogAcquisitionImageLoader()
  {
    Simplified.checkInitialized();
    return NullCheck.notNull(this.catalog_acquisition_image_loader);
  }

  public URI getFeedInitialURI()
  {
    Simplified.checkInitialized();
    return NullCheck.notNull(this.feed_initial_uri);
  }

  public OPDSFeedLoaderType getFeedLoader()
  {
    Simplified.checkInitialized();
    return NullCheck.notNull(this.feed_loader);
  }

  public BitmapCacheScalingType getImageLoader()
  {
    Simplified.checkInitialized();
    return NullCheck.notNull(this.image_loader);
  }

  public ListeningExecutorService getListeningExecutorService()
  {
    Simplified.checkInitialized();
    return NullCheck.notNull(this.exec_decor);
  }

  @Override public int memoryGetSize()
  {
    Simplified.checkInitialized();
    return this.memory;
  }

  @Override public boolean memoryIsSmall()
  {
    Simplified.checkInitialized();
    return this.memory_small;
  }

  @Override public void onCreate()
  {
    try {
      super.onCreate();

      Log.d(Simplified.TAG, "initializing application context");

      final ExecutorService e = Simplified.namedThreadPool();
      final Resources rr = NullCheck.notNull(this.getResources());

      /**
       * Determine memory conditions.
       */

      final ActivityManager am =
        (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
      this.memory = am.getMemoryClass();
      this.memory_small = this.memory <= 32;
      Log.d(Simplified.TAG, String.format(
        "available memory: %dmb (small: %s)",
        this.memory,
        this.memory_small));

      /**
       * Asynchronous feed loader.
       */

      final OPDSFeedParserType p = OPDSFeedParser.newParser();
      final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
      final OPDSFeedLoaderType flx = OPDSFeedLoader.newLoader(e, p, t);
      final OPDSFeedLoaderType fl = CachingFeedLoader.newLoader(flx);

      /**
       * Asynchronous image loader.
       */

      final PartialFunctionType<URI, InputStream, IOException> it =
        BitmapTransport.get();
      final File cd = new File(this.getExternalCacheDir(), "thumbnails");
      cd.mkdirs();

      final int disk_size =
        Simplified.megabytesToBytes(rr
          .getInteger(R.integer.image_disk_cache_megabytes));
      final int mem_size =
        Simplified.megabytesToBytes(rr
          .getInteger(R.integer.image_memory_cache_megabytes));

      final BitmapCacheScalingDiskType bcf =
        BitmapCacheScalingDisk.newCache(e, it, this, cd, disk_size);

      BitmapCacheScalingType bc;
      if (this.memory_small) {
        bc = bcf;
      } else {
        Log.d(
          Simplified.TAG,
          "non-small heap detected, using extra in-memory bitmap cache");
        bc = BitmapCacheScalingMemoryProxy.newCache(bcf, mem_size);
      }

      final CatalogAcquisitionImageCacheType cai =
        CatalogAcquisitionImageCache.newCache(bc);

      /**
       * Catalog URIs.
       */

      this.feed_initial_uri =
        URI.create(rr.getString(R.string.catalog_start_uri));

      this.executor = e;
      this.exec_decor =
        NullCheck.notNull(MoreExecutors.listeningDecorator(e));
      this.feed_parser = p;
      this.feed_transport = t;
      this.feed_loader = fl;
      this.image_loader = bcf;
      this.catalog_acquisition_image_loader = cai;

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
    Simplified.checkInitialized();
    final float scale = this.getResources().getDisplayMetrics().density;
    return ((dp * scale) + 0.5);
  }

  @Override public double screenGetDPI()
  {
    Simplified.checkInitialized();
    final DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    return metrics.densityDpi;
  }

  @Override public boolean screenIsLarge()
  {
    Simplified.checkInitialized();

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
