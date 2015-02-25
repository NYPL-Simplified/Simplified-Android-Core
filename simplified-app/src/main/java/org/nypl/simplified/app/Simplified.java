package org.nypl.simplified.app;

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

import android.app.Application;
import android.content.res.Resources;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * Global application state.
 */

public final class Simplified extends Application
{
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

  private static int megabytes(
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

  private @Nullable ExecutorService       executor;
  private @Nullable URI                   feed_initial_uri;
  private @Nullable OPDSFeedLoaderType    feed_loader;
  private @Nullable OPDSFeedParserType    feed_parser;
  private @Nullable OPDSFeedTransportType feed_transport;

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

  @Override public void onCreate()
  {
    super.onCreate();

    Log.d("simplified", "initializing application context");

    final ExecutorService e = Simplified.namedThreadPool();

    /**
     * Asynchronous feed loader.
     */

    final OPDSFeedParserType p = OPDSFeedParser.newParser();
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    final OPDSFeedLoaderType flx = OPDSFeedLoader.newLoader(e, p, t);
    final OPDSFeedLoaderType fl = CachingFeedLoader.newLoader(flx);

    /**
     * Catalog URIs.
     */

    final Resources rr = this.getResources();
    this.feed_initial_uri =
      URI.create(rr.getString(R.string.catalog_start_uri));

    this.executor = e;
    this.feed_parser = p;
    this.feed_transport = t;
    this.feed_loader = fl;
    Simplified.INSTANCE = this;
  }
}
