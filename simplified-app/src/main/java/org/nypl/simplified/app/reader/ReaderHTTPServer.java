package org.nypl.simplified.app.reader;

import java.util.concurrent.ExecutorService;

import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import fi.iki.elonen.NanoHTTPD;

/**
 * The default implementation of the {@link ReaderHTTPServerType} interface.
 */

public final class ReaderHTTPServer extends NanoHTTPD implements
  ReaderHTTPServerType
{
  private static final String TAG = "RHS";

  public static ReaderHTTPServerType newServer(
    final ExecutorService in_exec,
    final int in_port)
  {
    return new ReaderHTTPServer(in_exec, in_port);
  }

  private static Response serveActual(
    final IHTTPSession nns)
    throws Exception
  {
    Log.d(
      ReaderHTTPServer.TAG,
      String.format("request: %s %s", nns.getMethod(), nns.getUri()));
    return new Response(Response.Status.NOT_FOUND, "text/plain", "");
  }

  private static Response serveError(
    final Throwable x)
  {
    Log.e(ReaderHTTPServer.TAG, x.getMessage(), x);
    return new Response(
      Response.Status.INTERNAL_ERROR,
      "text/plain",
      x.getMessage());
  }

  private final ExecutorService exec;

  private ReaderHTTPServer(
    final ExecutorService in_exec,
    final int in_port)
  {
    super("127.0.0.1", in_port);
    this.exec = NullCheck.notNull(in_exec);
  }

  @Override public Response serve(
    final @Nullable IHTTPSession session)
  {
    try {
      final IHTTPSession nns = NullCheck.notNull(session);
      return ReaderHTTPServer.serveActual(nns);
    } catch (final Throwable x) {
      return ReaderHTTPServer.serveError(x);
    }
  }

  @Override public void start(
    final ReaderHTTPServerStartListenerType s)
  {
    NullCheck.notNull(s);
    final ReaderHTTPServer hs = this;
    this.exec.submit(new Runnable() {
      @Override public void run()
      {
        try {
          hs.start();
          s.onServerStartSucceeded(hs);
        } catch (final Throwable x) {
          s.onServerStartFailed(hs, x);
        }
      }
    });
  }
}
