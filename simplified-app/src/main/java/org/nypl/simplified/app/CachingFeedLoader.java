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

import com.io7m.jnull.NullCheck;

public final class CachingFeedLoader implements OPDSFeedLoaderType
{
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

  public static OPDSFeedLoaderType newLoader(
    final OPDSFeedLoaderType a)
  {
    return new CachingFeedLoader(a);
  }

  @Override public void fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    final Map<URI, OPDSFeedType> c = this.cache;
    if (c.containsKey(uri)) {
      final OPDSFeedType r = NullCheck.notNull(c.get(uri));
      p.onSuccess(r);
    } else {
      this.actual.fromURI(uri, new OPDSFeedLoadListenerType() {
        @Override public void onSuccess(
          final OPDSFeedType f)
        {
          c.put(uri, f);
          p.onSuccess(f);
        }

        @Override public void onFailure(
          final Exception e)
        {
          p.onFailure(e);
        }
      });
    }
  }

}
