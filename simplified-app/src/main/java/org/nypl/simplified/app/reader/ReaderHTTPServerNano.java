package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.http.core.HTTPRangeType;
import org.nypl.simplified.http.core.HTTPRanges;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.util.ResourceInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * The NanoHTTPd implementation of the {@link ReaderHTTPServerType} interface.
 */

public final class ReaderHTTPServerNano
  extends NanoHTTPD implements ReaderHTTPServerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderHTTPServerNano.class);
  }

  private final     URI                   base;
  private final     ExecutorService       exec;
  private final     ReaderHTTPMimeMapType mime;
  private final     ExecutorService       exec_request;
  private @Nullable Package               epub_package;

  private ReaderHTTPServerNano(
    final ExecutorService in_exec,
    final ExecutorService in_request_exec,
    final ReaderHTTPMimeMapType in_mime,
    final int in_port)
  {
    super("127.0.0.1", in_port);
    this.exec = NullCheck.notNull(in_exec);
    this.exec_request = NullCheck.notNull(in_request_exec);
    this.mime = NullCheck.notNull(in_mime);
    this.base =
      NullCheck.notNull(URI.create("http://127.0.0.1:" + in_port + "/"));

    this.setAsyncRunner(
      new AsyncRunner()
      {
        @Override public void exec(final Runnable code)
        {
          ReaderHTTPServerNano.this.exec_request.submit(code);
        }
      });
  }



  private static Response loggedResponse(
    final String path,
    final Response r)
  {
    final long thread_id = Thread.currentThread().getId();
    ReaderHTTPServerNano.LOG.debug(
      "response: [{}] {} {} {}",
      thread_id,
      r.getStatus(),
      path,
      r.getMimeType());
    return r;
  }

  /**
   * Construct a new HTTP server.
   *
   * @param in_exec         An executor service
   * @param in_request_exec An executor service that will be used to execute
   *                        HTTP requests
   * @param in_mime         The MIME map
   * @param in_port         The TCP port on which to listen
   *
   * @return A new server
   */

  public static ReaderHTTPServerType newServer(
    final ExecutorService in_exec,
    final ExecutorService in_request_exec,
    final ReaderHTTPMimeMapType in_mime,
    final int in_port)
  {
    return new ReaderHTTPServerNano(in_exec, in_request_exec, in_mime, in_port);
  }

  private static Response serveError(
    final Throwable x)
  {
    ReaderHTTPServerNano.LOG.error("{}", x.getMessage(), x);
    return new Response(
      Response.Status.INTERNAL_ERROR, "text/plain", x.getMessage());
  }

  @Override public URI getURIBase()
  {
    return this.base;
  }

  @Override public Response serve(
    final @Nullable IHTTPSession session)
  {
    try {
      final IHTTPSession nns = NullCheck.notNull(session);
      return this.serveActual(nns);
    } catch (final Throwable x) {
      return ReaderHTTPServerNano.serveError(x);
    }
  }

  private Response serveActual(
    final IHTTPSession nns)
    throws Exception
  {
    final ReaderNativeCodeReadLock read_lock = ReaderNativeCodeReadLock.get();
    final Method method = NullCheck.notNull(nns.getMethod());
    final String path = NullCheck.notNull(nns.getUri());
    final long thread_id = Thread.currentThread().getId();

    /**
     * Determine if the current request is a range request.
     */

    final OptionType<HTTPRangeType> range_opt =
      this.getRangeRequestType(nns, method, path, thread_id);

    /**
     * Guess the mime type.
     */

    final String type = this.mime.guessMimeTypeForURI(nns.getUri());

    /**
     * Try looking at the included Java resources first. This includes all of
     * the readium shared javascript content. For resources served in this
     * manner, range requests are ignored and the full entity is always
     * served.
     */

    {
      final InputStream stream =
        ReaderHTTPServerNano.class.getResourceAsStream(path);

      if (stream != null) {
        final Response response = new Response(Status.OK, type, stream);
        return ReaderHTTPServerNano.loggedResponse(path, response);
      }
    }

    /**
     * Otherwise, try serving it from the package. Range requests are
     * respected iff they are satisfiable.
     */

    {
      final Package pack = NullCheck.notNull(this.epub_package);
      final String relative = path.replaceFirst("^[/]+", "");

      ReaderHTTPServerNano.LOG.debug("request: trying package path: {}", relative);

      final int size;
      synchronized (read_lock) {
        size = pack.getArchiveInfoSize(relative);
      }

      if (size >= 0) {

        /**
         * Return a byte range stream that allows for very fine-grained
         * locking (locks are acquired during read operations and released
         * afterwards).
         */

        final Status status;
        final InputStream response_stream;
        final boolean is_range;
        if (range_opt.isSome()) {
          final Some<HTTPRangeType> some_range =
            (Some<HTTPRangeType>) range_opt;
          status = Response.Status.PARTIAL_CONTENT;
          is_range = true;
        } else {
          status = Response.Status.OK;
          is_range = false;
        }

        synchronized (read_lock) {
          final ResourceInputStream stream = NullCheck.notNull(
            (ResourceInputStream) pack.getInputStream(relative, is_range));
          response_stream =
            new ReaderHTTPByteRangeInputStream(stream, is_range, read_lock);
        }

        final Response response = new Response(status, type, response_stream);
        return ReaderHTTPServerNano.loggedResponse(path, response);
      }
    }

    return ReaderHTTPServerNano.loggedResponse(
      path, new Response(
        Response.Status.NOT_FOUND, "text/plain", ""));
  }

  private OptionType<HTTPRangeType> getRangeRequestType(
    final IHTTPSession nns,
    final Method method,
    final String path,
    final long thread_id)
  {
    final OptionType<HTTPRangeType> range_opt;
    final Map<String, String> headers = nns.getHeaders();

    if (headers.containsKey("range")) {
      final String range_text = NullCheck.notNull(headers.get("range"));
      range_opt = HTTPRanges.fromRangeString(range_text);
      if (range_opt.isSome()) {
        final Some<HTTPRangeType> some = (Some<HTTPRangeType>) range_opt;
        ReaderHTTPServerNano.LOG.debug(
          "request [{}] (ranged {}): {} {}",
          thread_id,
          some.get(),
          method,
          path);
      } else {
        ReaderHTTPServerNano.LOG.debug(
          "request [{}] (full - ranged unparseable): {} {}",
          thread_id,
          method,
          path);
      }
    } else {
      range_opt = Option.none();
      ReaderHTTPServerNano.LOG.debug(
        "request [{}] (full): {} {}", thread_id, method, path);
    }
    return range_opt;
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
    final ReaderHTTPServerNano hs = this;
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

  @Override public void close()
    throws IOException
  {
    LOG.debug("shutting down server");
    this.stop();
  }
}
