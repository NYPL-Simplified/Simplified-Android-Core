package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.Builder;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.slf4j.Logger;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An implementation of {@link OPDSFeedLoaderType} that caches successful
 * fetches.
 */

@SuppressWarnings("synthetic-access") public final class CachingFeedLoader implements
  OPDSFeedLoaderType
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
    LOG = LogUtilities.getLog(CachingFeedLoader.class);
  }

  /**
   * Construct a new loader.
   *
   * @param a
   *          The original loader
   * @return A new loader
   */

  public static OPDSFeedLoaderType newLoader(
    final OPDSFeedLoaderType a)
  {
    return new CachingFeedLoader(a);
  }

  private final OPDSFeedLoaderType     actual;
  private final Map<URI, OPDSFeedType> cache;

  private CachingFeedLoader(
    final OPDSFeedLoaderType a)
  {
    this.actual = NullCheck.notNull(a);
    final Builder<Object, Object> b = ExpiringMap.builder();
    b.expirationPolicy(ExpirationPolicy.ACCESSED);
    b.expiration(5, TimeUnit.MINUTES);
    final ExpiringMap<URI, OPDSFeedType> m = b.build();
    this.cache = NullCheck.notNull(m);
  }

  @Override public Future<Unit> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    CachingFeedLoader.LOG.debug("get: {}", uri);

    final Map<URI, OPDSFeedType> c = this.cache;
    if (c.containsKey(uri)) {
      CachingFeedLoader.LOG.debug("already-cached: {}", uri);

      final OPDSFeedType r = NullCheck.notNull(c.get(uri));
      final ImmediateFuture<Unit> f = new ImmediateFuture<Unit>(Unit.unit());
      try {
        p.onFeedLoadingSuccess(r);
      } catch (final Throwable x) {
        try {
          p.onFeedLoadingFailure(x);
        } catch (final Exception xe) {
          CachingFeedLoader.LOG.error(
            "listener onFeedLoadingFailure raised error: ",
            xe);
        }
      }
      return NullCheck.notNull(f);
    }

    return this.actual.fromURI(uri, new OPDSFeedLoadListenerType() {
      @Override public void onFeedLoadingFailure(
        final Throwable e)
      {
        CachingFeedLoader.LOG.debug("failed: {} ({})", uri, e.getMessage());
        p.onFeedLoadingFailure(e);
      }

      @Override public void onFeedLoadingSuccess(
        final OPDSFeedType f)
      {
        CachingFeedLoader.LOG.debug("received: {}", uri);
        c.put(uri, f);
        p.onFeedLoadingSuccess(f);
      }
    });
  }

}
