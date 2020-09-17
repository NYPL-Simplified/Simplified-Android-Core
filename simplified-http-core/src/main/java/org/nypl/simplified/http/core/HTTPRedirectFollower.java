package org.nypl.simplified.http.core;

import com.google.common.base.Preconditions;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A function to follow redirects and make some attempt to correctly handle
 * authentication.
 */

public final class HTTPRedirectFollower
  implements HTTPResultMatcherType<InputStream, HTTPResultType<InputStream>,
  Exception>
{
  private final long                     byte_offset;
  private final HTTPType                 http;
  private final String                   method;
  private final int                      max_redirects;
  private final OptionType<HTTPAuthType> target_auth;
  private final Set<URI>                 tried_auth;
  private final Logger                   logger;
  private       int                      cur_redirects;
  private       OptionType<HTTPAuthType> current_auth;
  private       URI                      current_uri;
  private       boolean                  used;

  /**
   * Construct a redirect follower capable of making a request to the given
   * URI.
   *
   * @param in_logger        A log interface
   * @param in_http          An HTTP interface
   * @param in_method        HTTP method to use (GET/PUT)
   * @param in_auth          Authentication info
   * @param in_max_redirects The maximum number of redirects to follow
   * @param in_uri           The target URI
   * @param in_byte_offset   The byte offset of the request
   */

  public HTTPRedirectFollower(
    final Logger in_logger,
    final HTTPType in_http,
    final String in_method,
    final OptionType<HTTPAuthType> in_auth,
    final int in_max_redirects,
    final URI in_uri,
    final long in_byte_offset)
  {
    this.logger = NullCheck.notNull(in_logger);
    this.http = NullCheck.notNull(in_http);
    this.method = NullCheck.notNull(in_method);
    this.target_auth = NullCheck.notNull(in_auth);
    this.current_auth = Option.none();
    this.current_uri = NullCheck.notNull(in_uri);
    this.max_redirects = in_max_redirects;
    this.cur_redirects = 0;
    this.byte_offset = in_byte_offset;
    this.tried_auth = new HashSet<URI>(32);
  }

  /**
   * @return The result of a successful HTTP request
   *
   * @throws Exception On any error
   */

  public HTTPResultOKType<InputStream> runExceptional()
    throws Exception
  {
    Preconditions.checkArgument(this.used == false, "Follower already used");

    try {
      final HTTPResultType<InputStream> r = this.processURI();
      return r.matchResult(
        new DownloadErrorFlattener<InputStream, HTTPResultOKType<InputStream>>(
          this.logger, this.current_uri)
        {
          @Override public HTTPResultOKType<InputStream> onHTTPOK(
            final HTTPResultOKType<InputStream> e) {
            return e;
          }
        });
    } finally {
      this.used = true;
    }
  }

  /**
   * @return The result of the HTTP requests
   */

  public HTTPResultType<InputStream> run()
  {
    Preconditions.checkArgument(this.used == false, "Follower already used");

    try {
      try {
        return this.processURI();
      } catch (final Exception e) {
        return new HTTPResultException<InputStream>(this.current_uri, e);
      }

    } finally {
      this.used = true;
    }
  }

  @Override public HTTPResultType<InputStream> onHTTPError(
    final HTTPResultError<InputStream> e)
    throws Exception
  {
    final int code = e.getStatus();
    this.logger.debug("received {} for {}", code, this.current_uri);

    HTTPProblemReportLogging.INSTANCE.logError(
      this.logger, this.current_uri, e.getMessage(), e.getStatus(), e.getProblemReport());

    switch (code) {
      case HttpURLConnection.HTTP_FORBIDDEN:
      case HttpURLConnection.HTTP_UNAUTHORIZED: {
        if (this.tried_auth.contains(this.current_uri)) {
          this.logger.error(
            "already tried authenticating for {}", this.current_uri);
          return e;
        }

        this.current_auth = this.target_auth;
        this.tried_auth.add(this.current_uri);
        return this.processURI();
      }
    }

    throw new IOException(String.format("%d: %s", code, e.getMessage()));
  }

  @Override public HTTPResultType<InputStream> onHTTPException(
    final HTTPResultException<InputStream> e)
    throws Exception
  {
    throw e.getError();
  }

  @Override public HTTPResultType<InputStream> onHTTPOK(
    final HTTPResultOKType<InputStream> e)
    throws Exception
  {
    final int code = e.getStatus();
    this.logger.debug("received {} for {}", code, this.current_uri);

    if (code >= 200 && code < 300) {
      return e;
    }

    switch (code) {
      case HttpURLConnection.HTTP_MOVED_PERM:
      case HttpURLConnection.HTTP_MOVED_TEMP:
      case HttpURLConnection.HTTP_SEE_OTHER:
      case 307:
      case 308: {

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

        final URI previous_uri = this.current_uri;

        // Resolve against the previous URI to handle relative redirects.
        this.current_uri = NullCheck.notNull(previous_uri.resolve(location));

        final boolean isSameHost =
          previous_uri.getHost().equals(this.current_uri.getHost());

        final boolean droppedHTTPS =
          "https".equals(previous_uri.getHost()) && !"https".equals(this.current_uri.getScheme());

        // Protect against passing authentication information in an unsafe manner.
        if (!isSameHost || droppedHTTPS) {
          this.current_auth = Option.none();
        }

        this.logger.debug(
          "following redirect {} to {}", this.cur_redirects, this.current_uri);

        return this.processURI();
      }
    }

    throw new IOException(
      String.format(
        "Unhandled http code (%d: %s)", e.getStatus(), e.getMessage()));
  }

  private HTTPResultType<InputStream> processURI()
    throws IOException, Exception
  {
    this.logger.debug("processing {}", this.current_uri);

    if (this.cur_redirects >= this.max_redirects) {
      throw new IOException("Reached redirect limit");
    }

    final HTTPResultType<InputStream> r;
    if ("PUT".equals(this.method)) {
      r = this.http.put(this.current_auth, this.current_uri);
    } else {
      r = this.http.get(this.current_auth, this.current_uri, 0L);
    }
    return r.matchResult(this);
  }

  private abstract static class DownloadErrorFlattener<A, B>
    implements HTTPResultMatcherType<A, B, Exception>
  {
    private final Logger logger;
    private final URI uri;

    private DownloadErrorFlattener(
      final Logger in_logger,
      final URI current_uri)
    {
      this.logger = Objects.requireNonNull(in_logger, "Logger");
      this.uri = Objects.requireNonNull(current_uri, "current_uri");
    }

    @Override public final B onHTTPError(
      final HTTPResultError<A> e)
      throws Exception
    {
      HTTPProblemReportLogging.INSTANCE.logError(
        this.logger, this.uri, e.getMessage(), e.getStatus(), e.getProblemReport());

      throw new IOException(String.format("%d: %s", e.getStatus(), e.getMessage()));
    }

    @Override public final B onHTTPException(
      final HTTPResultException<A> e)
      throws Exception
    {
      throw e.getError();
    }
  }
}
