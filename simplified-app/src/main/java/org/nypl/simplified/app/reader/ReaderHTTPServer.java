package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.http.core.HTTPRangeType;
import org.nypl.simplified.http.core.HTTPRanges;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.util.ResourceInputStream;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link ReaderHTTPServerType} interface.
 */

@SuppressWarnings("synthetic-access") public final class ReaderHTTPServer
  extends NanoHTTPD implements ReaderHTTPServerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderHTTPServer.class);
  }

  private final     URI                   base;
  private final     ExecutorService       exec;
  private final     ReaderHTTPMimeMapType mime;
  private @Nullable Package               epub_package;

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

  private static OptionType<String> getSuffix(
    final String path)
  {
    final int i = path.lastIndexOf('.');
    if (i > 0) {
      return Option.some(NullCheck.notNull(path.substring(i + 1)));
    }
    return Option.none();
  }

  private static Response loggedResponse(
    final String path,
    final Response r)
  {
    ReaderHTTPServer.LOG.debug(
      "response: {} {} {}", r.getStatus(), path, r.getMimeType());
    return r;
  }

  /**
   * Construct a new HTTP server.
   *
   * @param in_exec An executor service
   * @param in_mime The MIME map
   * @param in_port The TCP port on which to listen
   *
   * @return A new server
   */

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
    ReaderHTTPServer.LOG.error("{}", x.getMessage(), x);
    return new Response(
      Response.Status.INTERNAL_ERROR, "text/plain", x.getMessage());
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

    final OptionType<HTTPRangeType> range_opt;
    final Map<String, String> headers = nns.getHeaders();
    if (headers.containsKey("range")) {
      final String range_text = NullCheck.notNull(headers.get("range"));
      range_opt = HTTPRanges.fromRangeString(range_text);
      if (range_opt.isSome()) {
        final Some<HTTPRangeType> some = (Some<HTTPRangeType>) range_opt;
        ReaderHTTPServer.LOG.debug(
          "request (ranged {}): {} {}", some.get(), method, path);
      } else {
        ReaderHTTPServer.LOG.debug(
          "request (full - ranged unparseable): {} {}", method, path);
      }
    } else {
      range_opt = Option.none();
      ReaderHTTPServer.LOG.debug("request (full): {} {}", method, path);
    }

    final String type = this.guessMimeTime(path);

    /**
     * Try looking at the included Java resources first. This includes all of
     * the readium shared javascript content. For resources served in this
     * manner, range requests are ignored and the full entity is always
     * served.
     */

    {
      final InputStream stream =
        ReaderHTTPServer.class.getResourceAsStream(path);

      if (stream != null) {
        final Response response = new Response(Status.OK, type, stream);
        return ReaderHTTPServer.loggedResponse(path, response);
      }
    }

    /**
     * Otherwise, try serving it from the package. Range requests are
     * respected iff they are satisfiable.
     */

    {
      final Package pack = NullCheck.notNull(this.epub_package);
      final String relative = path.replaceFirst("^[/]+", "");

      ReaderHTTPServer.LOG.debug("request: trying package path: {}", relative);

      final int size = pack.getArchiveInfoSize(relative);
      if (size >= 0) {

        final ResourceInputStream stream;
        final Status status;
        if (range_opt.isSome()) {
          final Some<HTTPRangeType> some_range =
            (Some<HTTPRangeType>) range_opt;

          status = Response.Status.PARTIAL_CONTENT;
          stream = NullCheck.notNull(
            (ResourceInputStream) pack.getInputStream(relative, true));
        } else {
          status = Response.Status.OK;
          stream = NullCheck.notNull(
            (ResourceInputStream) pack.getInputStream(relative, false));
        }

        final Response response = new Response(status, type, stream);
        return ReaderHTTPServer.loggedResponse(path, response);
      }
    }

    return ReaderHTTPServer.loggedResponse(
      path, new Response(
        Response.Status.NOT_FOUND, "text/plain", ""));
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
    this.exec.submit(
      new Runnable()
      {
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
