package org.nypl.simplified.books.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.Builder;
import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;

import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings("synthetic-access") public final class FeedLoader implements
  FeedLoaderType,
  ExpirationListener<URI, FeedType>
{
  private static final class ImmediateFuture<T> implements Future<T>
  {
    private final T value;

    private ImmediateFuture(
      final T in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    @Override public boolean cancel(
      final boolean x)
    {
      return false;
    }

    @Override public T get()
      throws InterruptedException,
        ExecutionException
    {
      return this.value;
    }

    @Override public T get(
      final long time,
      final @Nullable TimeUnit time_unit)
      throws InterruptedException,
        ExecutionException,
        TimeoutException
    {
      return this.value;
    }

    @Override public boolean isCancelled()
    {
      return false;
    }

    @Override public boolean isDone()
    {
      return true;
    }
  }

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FeedLoader.class));
  }

  private static void callErrorListener(
    final URI uri,
    final FeedLoaderListenerType listener,
    final Throwable x)
  {
    try {
      listener.onFeedLoadFailure(uri, x);
    } catch (final Throwable xe) {
      FeedLoader.LOG.error("listener raised error: ", xe);
    }
  }

  private static void callListener(
    final URI uri,
    final FeedLoaderListenerType listener,
    final FeedType f)
  {
    try {
      listener.onFeedLoadSuccess(uri, f);
    } catch (final Throwable x) {
      FeedLoader.callErrorListener(uri, listener, x);
    }
  }

  public static FeedLoaderType newFeedLoader(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType in_transport)
  {
    final Builder<Object, Object> b = ExpiringMap.builder();
    b.expirationPolicy(ExpirationPolicy.ACCESSED);
    b.expiration(5, TimeUnit.MINUTES);
    final ExpiringMap<URI, FeedType> m = b.build();
    return FeedLoader.newFeedLoaderFromExpiringMap(
      in_exec,
      in_parser,
      in_transport,
      NullCheck.notNull(m));
  }

  public static FeedLoaderType newFeedLoaderFromExpiringMap(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType in_transport,
    final ExpiringMap<URI, FeedType> m)
  {
    return new FeedLoader(in_exec, in_parser, in_transport, m);
  }

  private final ExpiringMap<URI, FeedType> cache;
  private final ExecutorService            exec;
  private final OPDSFeedParserType         parser;
  private final OPDSFeedTransportType      transport;

  private FeedLoader(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType in_transport,
    final ExpiringMap<URI, FeedType> in_m)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.parser = NullCheck.notNull(in_parser);
    this.transport = NullCheck.notNull(in_transport);
    this.cache = NullCheck.notNull(in_m);
    this.cache.addExpirationListener(this);
  }

  @Override public void expired(
    final @Nullable URI key,
    final @Nullable FeedType value)
  {
    FeedLoader.LOG.debug("expired: {}", key);
  }

  private Future<Unit> fetch(
    final URI uri,
    final FeedLoaderListenerType listener)
  {
    FeedLoader.LOG.debug("not cached, fetching: {}", uri);

    final Callable<Unit> c = new Callable<Unit>() {
      @Override public Unit call()
      {
        try {
          final FeedType f = FeedLoader.this.loadFeed(uri);
          FeedLoader.this.cache.put(uri, f);
          FeedLoader.LOG.debug("added to cache: {}", uri);
          FeedLoader.callListener(uri, listener, f);
        } catch (final Throwable x) {
          FeedLoader.callErrorListener(uri, listener, x);
        }

        return Unit.unit();
      }
    };

    return NullCheck.notNull(this.exec.submit(c));
  }

  @Override public Future<Unit> fromURI(
    final URI uri,
    final FeedLoaderListenerType listener)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(listener);

    if (this.cache.containsKey(uri)) {
      FeedLoader.LOG.debug("retrieved from cache: {}", uri);
      final FeedType f = NullCheck.notNull(this.cache.get(uri));
      FeedLoader.callListener(uri, listener, f);
      return new ImmediateFuture<Unit>(Unit.unit());
    }

    return this.fetch(uri, listener);
  }

  @Override public Future<Unit> fromURIRefreshing(
    final URI uri,
    final FeedLoaderListenerType listener)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(listener);
    return this.fetch(uri, listener);
  }

  private FeedType loadFeed(
    final URI uri)
    throws IOException
  {
    final InputStream s = this.transport.getStream(uri);
    try {
      return Feeds.fromAcquisitionFeed(this.parser.parse(uri, s));
    } finally {
      s.close();
    }
  }
}
