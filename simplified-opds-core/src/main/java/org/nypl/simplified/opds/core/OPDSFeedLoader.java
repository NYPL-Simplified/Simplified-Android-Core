package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link OPDSFeedLoaderType}.
 */

@SuppressWarnings("synthetic-access") public final class OPDSFeedLoader implements
  OPDSFeedLoaderType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(OPDSFeedLoader.class));
  }

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

  @Override public Future<Unit> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(p);

    final OPDSFeedTransportType ref_t = this.transport;
    final OPDSFeedParserType ref_p = this.parser;

    final Future<Unit> f = this.exec.submit(new Callable<Unit>() {
      @Override public Unit call()
      {
        InputStream s = null;
        try {
          s = ref_t.getStream(uri);
          final OPDSFeedType r = ref_p.parse(uri, s);
          p.onFeedLoadingSuccess(r);
        } catch (final Throwable e) {
          try {
            p.onFeedLoadingFailure(e);
          } catch (final Throwable ex) {
            OPDSFeedLoader.LOG.error("listener raised exception: ", ex);
          }
        } finally {
          if (s != null) {
            try {
              s.close();
            } catch (final IOException e) {
              OPDSFeedLoader.LOG.error(
                "raised exception during stream close: ",
                s);
            }
          }
        }

        return Unit.unit();
      }
    });

    return NullCheck.notNull(f);
  }
}
