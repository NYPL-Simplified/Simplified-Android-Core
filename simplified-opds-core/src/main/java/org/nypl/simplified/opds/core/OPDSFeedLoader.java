package org.nypl.simplified.opds.core;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link OPDSFeedLoaderType}.
 */

public final class OPDSFeedLoader implements OPDSFeedLoaderType
{
  public static OPDSFeedLoaderType newLoader(
    final ExecutorService e,
    final OPDSFeedParserType p,
    final OPDSFeedTransportType t)
  {
    return new OPDSFeedLoader(e, p, t);
  }
  private final ExecutorService       exec;
  private final OPDSFeedParserType    parser;

  private final OPDSFeedTransportType transport;

  private OPDSFeedLoader(
    final ExecutorService e,
    final OPDSFeedParserType p,
    final OPDSFeedTransportType t)
  {
    this.exec = NullCheck.notNull(e);
    this.parser = NullCheck.notNull(p);
    this.transport = NullCheck.notNull(t);
  }

  @Override public void fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    final OPDSFeedTransportType ref_t = this.transport;
    final OPDSFeedParserType ref_p = this.parser;
    this.exec.execute(new Runnable() {
      @Override public void run()
      {
        try {
          InputStream s = null;
          try {
            s = ref_t.getStream(uri);
            p.onSuccess(ref_p.parse(s));
          } finally {
            if (s != null) {
              s.close();
            }
          }
        } catch (final Exception e) {
          p.onFailure(e);
        }
      }
    });
  }
}
