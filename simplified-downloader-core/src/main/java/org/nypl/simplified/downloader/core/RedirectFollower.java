package org.nypl.simplified.downloader.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A function to follow redirects and make some attempt to correctly handle
 * authentication.
 */

@SuppressWarnings("boxing") final class RedirectFollower implements
  Callable<HTTPResultOKType<InputStream>>,
  HTTPResultMatcherType<Unit, Unit, Exception>
{
  private final long                     byte_offset;
  private int                            cur_redirects;
  private OptionType<HTTPAuthType>       current_auth;
  private URI                            current_uri;
  private final HTTPType                 http;
  private final int                      max_redirects;
  private final OptionType<HTTPAuthType> target_auth;
  private final Set<URI>                 tried_auth;
  private final Logger                   logger;

  RedirectFollower(
    final Logger in_logger,
    final HTTPType in_http,
    final OptionType<HTTPAuthType> in_auth,
    final int in_max_redirects,
    final URI in_uri,
    final long in_byte_offset)
  {
    this.logger = NullCheck.notNull(in_logger);
    this.http = NullCheck.notNull(in_http);
    this.target_auth = NullCheck.notNull(in_auth);
    this.current_auth = Option.none();
    this.current_uri = NullCheck.notNull(in_uri);
    this.max_redirects = in_max_redirects;
    this.cur_redirects = 0;
    this.byte_offset = in_byte_offset;
    this.tried_auth = new HashSet<URI>(32);
  }

  private abstract static class DownloadErrorFlattener<A, B> implements
    HTTPResultMatcherType<A, B, Exception>
  {
    private DownloadErrorFlattener()
    {

    }

    @Override public final B onHTTPError(
      final HTTPResultError<A> e)
      throws Exception
    {
      final String m = String.format("%d: %s", e.getStatus(), e.getMessage());
      throw new IOException(NullCheck.notNull(m));
    }

    @Override public final B onHTTPException(
      final HTTPResultException<A> e)
      throws Exception
    {
      throw e.getError();
    }
  }

  @Override public HTTPResultOKType<InputStream> call()
    throws Exception
  {
    this.processURI();

    final HTTPResultType<InputStream> r =
      this.http.get(this.current_auth, this.current_uri, this.byte_offset);

    return r
      .matchResult(new DownloadErrorFlattener<InputStream, HTTPResultOKType<InputStream>>() {
        @Override public HTTPResultOKType<InputStream> onHTTPOK(
          final HTTPResultOKType<InputStream> e)
          throws Exception
        {
          return e;
        }
      });
  }

  @Override public Unit onHTTPError(
    final HTTPResultError<Unit> e)
    throws Exception
  {
    final int code = e.getStatus();
    this.logger.debug("received {} for {}", code, this.current_uri);

    switch (code) {
      case HttpURLConnection.HTTP_UNAUTHORIZED:
      {
        if (this.tried_auth.contains(this.current_uri)) {
          this.logger.error(
            "already tried authenticating for {}",
            this.current_uri);

          final String m = String.format("%d: %s", code, e.getMessage());
          throw new DownloadAuthenticationError(NullCheck.notNull(m));
        }

        this.current_auth = this.target_auth;
        this.tried_auth.add(this.current_uri);
        this.processURI();
        return Unit.unit();
      }
    }

    final String m = String.format("%d: %s", code, e.getMessage());
    throw new IOException(NullCheck.notNull(m));
  }

  @Override public Unit onHTTPException(
    final HTTPResultException<Unit> e)
    throws Exception
  {
    throw e.getError();
  }

  @Override public Unit onHTTPOK(
    final HTTPResultOKType<Unit> e)
    throws Exception
  {
    final int code = e.getStatus();
    this.logger.debug("received {} for {}", code, this.current_uri);

    switch (code) {
      case HttpURLConnection.HTTP_OK:
      {
        return Unit.unit();
      }

      case HttpURLConnection.HTTP_MOVED_PERM:
      case HttpURLConnection.HTTP_MOVED_TEMP:
      case 307:
      case 308:
      {
        this.current_auth = Option.none();

        final Map<String, List<String>> headers =
          NullCheck.notNull(e.getResponseHeaders());
        final List<String> locations =
          NullCheck.notNull(headers.get("Location"));

        if (locations.size() != 1) {
          throw new IOException(
            "Malformed server response: Expected exactly one Location header");
        }

        final String location = NullCheck.notNull(locations.get(0));
        this.cur_redirects = this.cur_redirects + 1;
        this.current_uri = NullCheck.notNull(URI.create(location));

        this.logger.debug(
          "following redirect {} to {}",
          this.cur_redirects,
          this.current_uri);

        this.processURI();
        return Unit.unit();
      }
    }

    throw new IOException(String.format(
      "Unhandled http code (%d: %s)",
      e.getStatus(),
      e.getMessage()));
  }

  private void processURI()
    throws IOException,
      Exception
  {
    this.logger.debug("processing {}", this.current_uri);

    if (this.cur_redirects >= this.max_redirects) {
      throw new IOException("Reached redirect limit");
    }

    final HTTPResultType<Unit> r =
      this.http.head(this.current_auth, this.current_uri);
    r.matchResult(this);
  }
}
