package org.nypl.simplified.http.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Default implementation of the {@link HTTPType} type.
 */

public final class HTTP implements HTTPType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(HTTP.class));
  }

  private final String user_agent;

  private HTTP()
  {
    this.user_agent = HTTP.userAgent();
  }

  private static String userAgent()
  {
    final Package p = HTTP.class.getPackage();
    if (p != null) {
      final String v = p.getImplementationVersion();
      if (v != null) {
        return NullCheck.notNull(String.format("simplified-http %s", v));
      }
    }
    return "simplified-http";
  }

  private static void checkURI(
    final URI uri)
  {
    NullCheck.notNull(uri);
    final String scheme = NullCheck.notNull(uri.getScheme());
    final boolean ok = "http".equals(scheme) || "https".endsWith(scheme);
    if (!ok) {
      final StringBuilder m = new StringBuilder(64);
      m.append("Unsupported URI scheme.\n");
      m.append("  URI scheme: ");
      m.append(scheme);
      m.append("\n");
      m.append("  Supported schemes: http https\n");
      throw new IllegalArgumentException(m.toString());
    }
  }

  /**
   * @return A new HTTP interface
   */

  public static HTTPType newHTTP()
  {
    return new HTTP();
  }

  @Override public HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final long offset)
  {
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
      HTTP.LOG.trace("GET {} (auth {})", uri, auth_opt);

      final URL url = NullCheck.notNull(uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      conn.setInstanceFollowRedirects(false);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Range", "bytes=" + offset + "-");
      conn.setRequestProperty("User-Agent", this.user_agent);
      conn.setRequestProperty("Accept-Encoding", "identity");

      if (auth_opt.isSome()) {
        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        auth.setConnectionParameters(conn);
      }

      conn.connect();

      final int code = conn.getResponseCode();
      HTTP.LOG.trace(
        "GET {} (auth {}) (result {})", uri, auth_opt, Integer.valueOf(code));

      conn.getLastModified();
      if (code >= 400) {
        final OptionType<HTTPProblemReport> report =
          this.getReportFromError(conn);
        return new HTTPResultError<InputStream>(
          code,
          NullCheck.notNull(conn.getResponseMessage()),
          (long) conn.getContentLength(),
          NullCheck.notNull(conn.getHeaderFields()),
          conn.getLastModified(),
          this.getErrorStreamOrEmpty(conn),
          report);
      }

      return new HTTPResultOK<InputStream>(
        NullCheck.notNull(conn.getResponseMessage()),
        code,
        conn.getInputStream(),
        (long) conn.getContentLength(),
        NullCheck.notNull(conn.getHeaderFields()),
        conn.getLastModified());
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<InputStream>(uri, e);
    }
  }

  private OptionType<HTTPProblemReport> getReportFromError(
    final HttpURLConnection conn)
    throws IOException
  {
    final OptionType<HTTPProblemReport> report;
    if ("application/problem+json".equals(conn.getContentType())) {
      final HTTPProblemReport r =
        HTTPProblemReport.fromStream(conn.getErrorStream());
      report = Option.some(r);
    } else {
      report = Option.none();
    }
    return report;
  }

  @Override public HTTPResultType<Unit> head(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri)
  {
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
      HTTP.LOG.trace("HEAD {} (auth {})", uri, auth_opt);

      final URL url = NullCheck.notNull(uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      conn.setInstanceFollowRedirects(false);
      conn.setRequestMethod("HEAD");
      conn.setRequestProperty("User-Agent", this.user_agent);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Accept-Encoding", "identity");

      if (auth_opt.isSome()) {
        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        auth.setConnectionParameters(conn);
      }

      conn.connect();

      final int code = conn.getResponseCode();
      HTTP.LOG.trace(
        "HEAD {} (auth {}) (result {})", uri, auth_opt, Integer.valueOf(code));

      if (code >= 400) {
        final OptionType<HTTPProblemReport> report =
          this.getReportFromError(conn);
        return new HTTPResultError<Unit>(
          code,
          NullCheck.notNull(conn.getResponseMessage()),
          (long) conn.getContentLength(),
          NullCheck.notNull(conn.getHeaderFields()),
          conn.getLastModified(),
          this.getErrorStreamOrEmpty(conn),
          report);
      }

      return new HTTPResultOK<Unit>(
        NullCheck.notNull(conn.getResponseMessage()),
        code,
        Unit.unit(),
        (long) conn.getContentLength(),
        NullCheck.notNull(conn.getHeaderFields()),
        conn.getLastModified());
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<Unit>(uri, e);
    }
  }

  private InputStream getErrorStreamOrEmpty(final HttpURLConnection conn)
  {
    final InputStream stream = conn.getErrorStream();
    if (stream != null) {
      return stream;
    }
    return new ByteArrayInputStream(new byte[0]);
  }
}
