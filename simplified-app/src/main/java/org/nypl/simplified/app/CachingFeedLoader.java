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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jnull.NullCheck;

/**
 * An implementation of {@link OPDSFeedLoaderType} that caches successful
 * fetches.
 */

public final class CachingFeedLoader implements OPDSFeedLoaderType
{
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
    final Map<URI, OPDSFeedType> c = this.cache;
    if (c.containsKey(uri)) {
      final OPDSFeedType r = NullCheck.notNull(c.get(uri));
      final ListenableFuture<OPDSFeedType> f = Futures.immediateFuture(r);
      p.onSuccess(r);
      return f;
    }

    return this.actual.fromURI(uri, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Throwable e)
      {
        p.onFailure(e);
      }

      @Override public void onSuccess(
        final OPDSFeedType f)
      {
        c.put(uri, f);
        p.onSuccess(f);
      }
    });
  }

}
