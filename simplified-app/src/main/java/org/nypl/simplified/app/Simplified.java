package org.nypl.simplified.app;

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

  private static ExecutorService namedThreadPool()
  {
    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ExecutorService pool =
      Executors.newCachedThreadPool(new ThreadFactory() {
        private int id = 0;

        @SuppressWarnings("boxing") @Override public Thread newThread(
          final @Nullable Runnable r)
        {
          final Thread t = tf.newThread(r);
          t.setName(String.format("simplified-tasks-%d", this.id));
          ++this.id;
          return t;
        }
      });
    return NullCheck.notNull(pool);
  }

  private @Nullable ExecutorService       executor;
  private @Nullable OPDSFeedParserType    feed_parser;
  private @Nullable OPDSFeedTransportType feed_transport;
  private @Nullable OPDSFeedLoaderType    feed_loader;

  @Override public void onCreate()
  {
    super.onCreate();

    Log.d("simplified", "initializing application context");

    final ExecutorService e = Simplified.namedThreadPool();
    final OPDSFeedParserType p = OPDSFeedParser.newParser();
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    final OPDSFeedLoaderType fl = OPDSFeedLoader.newLoader(e, p, t);

    this.executor = e;
    this.feed_parser = p;
    this.feed_transport = t;
    this.feed_loader = fl;
    Simplified.INSTANCE = this;
  }
}
