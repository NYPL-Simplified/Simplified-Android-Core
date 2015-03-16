package org.nypl.simplified.app;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.Builder;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;

import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedType;

import android.util.Log;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jnull.NullCheck;

/**
 * An implementation of {@link OPDSFeedLoaderType} that caches successful
 * fetches.
 */

@SuppressWarnings("synthetic-access") public final class CachingFeedLoader implements
  OPDSFeedLoaderType
{
  private static final String TAG;

  static {
    TAG = "CFL";
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

  @Override public ListenableFuture<OPDSFeedType> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    Log.d(CachingFeedLoader.TAG, String.format("get %s", uri));

    final Map<URI, OPDSFeedType> c = this.cache;
    if (c.containsKey(uri)) {
      Log.d(CachingFeedLoader.TAG, String.format("already-cached %s", uri));
      final OPDSFeedType r = NullCheck.notNull(c.get(uri));
      final ListenableFuture<OPDSFeedType> f = Futures.immediateFuture(r);
      p.onFeedLoadingSuccess(r);
      return NullCheck.notNull(f);
    }

    return this.actual.fromURI(uri, new OPDSFeedLoadListenerType() {
      @Override public void onFeedLoadingFailure(
        final Throwable e)
      {
        Log.d(
          CachingFeedLoader.TAG,
          String.format("failed %s (%s)", uri, e.getMessage()));
        p.onFeedLoadingFailure(e);
      }

      @Override public void onFeedLoadingSuccess(
        final OPDSFeedType f)
      {
        Log.d(CachingFeedLoader.TAG, String.format("received %s", uri));
        c.put(uri, f);
        p.onFeedLoadingSuccess(f);
      }
    });
  }

}
