package org.nypl.simplified.app.reader;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.readium.sdk.android.Package;

import android.util.Log;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import fi.iki.elonen.NanoHTTPD;

/**
 * The default implementation of the {@link ReaderHTTPServerType} interface.
 */

@SuppressWarnings("synthetic-access") public final class ReaderHTTPServer extends
  NanoHTTPD implements ReaderHTTPServerType
{
  private static final String TAG = "RHS";

  private static OptionType<String> getSuffix(
    final String path)
  {
    final int i = path.lastIndexOf('.');
    if (i > 0) {
      return Option.some(path.substring(i + 1));
    }
    return Option.none();
  }

  private static Response loggedResponse(
    final String path,
    final Response r)
  {
    Log.d(
      ReaderHTTPServer.TAG,
      String.format(
        "response: %s %s %s",
        r.getStatus(),
        path,
        r.getMimeType()));
    return r;
  }

  public static ReaderHTTPServerType newServer(
    final ExecutorService in_exec,
    final ReaderHTTPMimeMapType in_mime,
    final int in_port)
  {
    return new ReaderHTTPServer(in_exec, in_mime, in_port);
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

  private final URI                   base;
  private @Nullable Package           epub_package;
  private final ExecutorService       exec;
  private final ReaderHTTPMimeMapType mime;

  private ReaderHTTPServer(
    final ExecutorService in_exec,
    final ReaderHTTPMimeMapType in_mime,
    final int in_port)
  {
    super("127.0.0.1", in_port);
    this.exec = NullCheck.notNull(in_exec);
    this.mime = NullCheck.notNull(in_mime);
    this.base =
      NullCheck.notNull(URI.create("http://127.0.0.1:" + in_port + "/"));
  }

  @Override public URI getURIBase()
  {
    return this.base;
  }

  private String guessMimeTime(
    final String uri)
  {
    final OptionType<String> opt = ReaderHTTPServer.getSuffix(uri);
    if (opt.isSome()) {
      final String suffix = ((Some<String>) opt).get();
      return this.mime.getMimeTypeForSuffix(suffix);
    }
    return this.mime.getDefaultMimeType();
  }

  @Override public Response serve(
    final @Nullable IHTTPSession session)
  {
    try {
      final IHTTPSession nns = NullCheck.notNull(session);
      return this.serveActual(nns);
    } catch (final Throwable x) {
      return ReaderHTTPServer.serveError(x);
    }
  }

  private Response serveActual(
    final IHTTPSession nns)
    throws Exception
  {
    final Method method = NullCheck.notNull(nns.getMethod());
    final String path = NullCheck.notNull(nns.getUri());

    Log
      .d(ReaderHTTPServer.TAG, String.format("request: %s %s", method, path));

    final String type = this.guessMimeTime(path);

    /**
     * Try looking at the included Java resources first. This includes all of
     * the readium shared javascript content.
     */

    {
      final InputStream stream =
        ReaderHTTPServer.class.getResourceAsStream(path);

      if (stream != null) {
        return ReaderHTTPServer.loggedResponse(path, new Response(
          Response.Status.OK,
          type,
          stream));
      }
    }

    /**
     * Otherwise, try serving it from the package.
     */

    {
      final Package pack = NullCheck.notNull(this.epub_package);

      final String relative = path.replaceFirst("^[/]+", "");
      final String package_path =
        String.format("%s%s", pack.getBasePath(), relative);

      Log.d(
        ReaderHTTPServer.TAG,
        String.format("request: trying package path: %s", relative));

      final InputStream stream = pack.getInputStream(relative, false);
      if (stream != null) {
        return ReaderHTTPServer.loggedResponse(path, new Response(
          Response.Status.OK,
          type,
          stream));
      }
    }

    return ReaderHTTPServer.loggedResponse(path, new Response(
      Response.Status.NOT_FOUND,
      "text/plain",
      ""));
  }

  private void setPackage(
    final Package p)
  {
    this.epub_package = NullCheck.notNull(p);
  }

  @Override public void startIfNecessaryForPackage(
    final Package p,
    final ReaderHTTPServerStartListenerType s)
  {
    NullCheck.notNull(p);
    NullCheck.notNull(s);
    final ReaderHTTPServer hs = this;
    this.exec.submit(new Runnable() {
      @Override public void run()
      {
        try {
          boolean fresh = false;
          if (hs.isAlive() == false) {
            hs.start();
            fresh = true;
          }
          hs.setPackage(p);
          s.onServerStartSucceeded(hs, fresh);
        } catch (final Throwable x) {
          s.onServerStartFailed(hs, x);
        }
      }
    });
  }
}
