package org.nypl.simplified.opds.core;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

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

  private final ListeningExecutorService exec;
  private final OPDSFeedParserType       parser;
  private final OPDSFeedTransportType    transport;

  private OPDSFeedLoader(
    final ExecutorService e,
    final OPDSFeedParserType p,
    final OPDSFeedTransportType t)
  {
    this.exec =
      NullCheck
        .notNull(MoreExecutors.listeningDecorator(NullCheck.notNull(e)));
    this.parser = NullCheck.notNull(p);
    this.transport = NullCheck.notNull(t);
  }

  @Override public ListenableFuture<OPDSFeedType> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(p);

    final OPDSFeedTransportType ref_t = this.transport;
    final OPDSFeedParserType ref_p = this.parser;

    final ListenableFuture<OPDSFeedType> f =
      this.exec.submit(new Callable<OPDSFeedType>() {
        @Override public OPDSFeedType call()
          throws Exception
        {
          InputStream s = null;
          try {
            s = ref_t.getStream(uri);
            return ref_p.parse(s);
          } finally {
            if (s != null) {
              s.close();
            }
          }
        }
      });

    Futures.addCallback(f, new FutureCallback<OPDSFeedType>() {
      @Override public void onFailure(
        final @Nullable Throwable t)
      {
        p.onFailure(NullCheck.notNull(t));
      }

      @Override public void onSuccess(
        final @Nullable OPDSFeedType result)
      {
        p.onSuccess(NullCheck.notNull(result));
      }
    }, this.exec);
    return NullCheck.notNull(f);
  }
}
