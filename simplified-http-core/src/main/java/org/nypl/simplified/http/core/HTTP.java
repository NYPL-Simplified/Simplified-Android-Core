package org.nypl.simplified.http.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the {@link HTTPType} type.
 */

public final class HTTP implements HTTPType {
  private static final Logger LOG;

  /**
   * The MIME type of HTTP problem reports.
   */

  public static final String HTTP_PROBLEM_REPORT_CONTENT_TYPE =
    "application/api-problem+json";

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(HTTP.class));
  }

  private final String user_agent;

  private HTTP() {
    this.user_agent = HTTP.userAgent();
  }

  public static String userAgent() {
    final Package p = HTTP.class.getPackage();
    if (p != null) {
      final String v = p.getImplementationVersion();
      if (v != null) {
        return NullCheck.notNull(String.format("simplified-http Android %s", v));
      }
    }
    return "simplified-http Android";
  }

  private static void checkURI(
    final URI uri) {
    NullCheck.notNull(uri);

    final String scheme = uri.getScheme();
    final boolean ok = scheme != null && scheme.startsWith("http");
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

  public static HTTPType newHTTP() {
    return new HTTP();
  }

  @Override
  public HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final long offset) {
    final OptionType<byte[]> data = Option.none();
    final OptionType<String> content_type = Option.none();
    return this.requestInternal("GET", auth_opt, uri, offset, data, content_type, false);
  }

  @Override
  public HTTPResultType<InputStream> get(
    OptionType<HTTPAuthType> auth_opt,
    URI uri,
    long offset,
    Boolean noCache) {
    final OptionType<byte[]> data = Option.none();
    final OptionType<String> content_type = Option.none();
    return this.requestInternal("GET", auth_opt, uri, offset, data, content_type, noCache);
  }

  @Override
  public HTTPResultType<InputStream> put(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri) {
    final OptionType<byte[]> data = Option.none();
    final OptionType<String> content_type = Option.none();
    return this.requestInternal("PUT", auth_opt, uri, 0, data, content_type, false);
  }

  @Override
  public HTTPResultType<InputStream> put(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final byte[] data,
    final String content_type) {
    return this.requestInternal("PUT", auth_opt, uri, 0, Option.some(data), Option.some(content_type), false);
  }

  @Override
  public HTTPResultType<InputStream> post(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final byte[] data,
    final String content_type) {
    return this.requestInternal("POST", auth_opt, uri, 0, Option.some(data), Option.some(content_type), false);
  }

  @Override
  public HTTPResultType<InputStream> delete(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final String content_type) {
    return this.requestInternal("DELETE", auth_opt, uri, 0, Option.<byte[]>none(), Option.some(content_type), false);
  }

  private HTTPResultType<InputStream> requestInternal(
    final String method,
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final long offset,
    final OptionType<byte[]> data_opt,
    final OptionType<String> content_type_opt,
    final Boolean noCache) {
    NullCheck.notNull(method);
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
      HTTP.LOG.trace("{} {} (auth {})", method, uri, auth_opt);

      final URL url = NullCheck.notNull(uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      conn.setInstanceFollowRedirects(false);
      conn.setRequestMethod(method);
      conn.setDoInput(true);
      conn.setReadTimeout(
        (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));
      if (offset > 0) {
        conn.setRequestProperty("Range", "bytes=" + offset + "-");
      }
      conn.setRequestProperty("User-Agent", this.user_agent);

      if (content_type_opt.isSome()) {
        conn.setRequestProperty("Content-Type", ((Some<String>) content_type_opt).get());
      }

      if (auth_opt.isSome()) {
        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        auth.setConnectionParameters(conn);
      }

      if (data_opt.isSome()) {
        final Some<byte[]> data_some = (Some<byte[]>) data_opt;
        final byte[] data = data_some.get();
        conn.setDoOutput(true);
        final OutputStream os = conn.getOutputStream();
        os.write(data);
        os.close();
      }

      if (noCache) {
        conn.addRequestProperty("Cache-Control", "no-cache");
      }

      conn.connect();

      int code = conn.getResponseCode();
      HTTP.LOG.trace("{} {} (auth {}) (result {})", method, uri, auth_opt, code);

      final StatusCodeOverride override = statusCodeOverrideFrom(conn);
      code = override.statusCode(code);

      conn.getLastModified();
      if (code >= 400) {
        return new HTTPResultError<InputStream>(
          code,
          NullCheck.notNull(conn.getResponseMessage()),
          (long) conn.getContentLength(),
          NullCheck.notNull(conn.getHeaderFields()),
          conn.getLastModified(),
          this.getErrorStreamOrEmpty(conn),
          override.reportOpt);
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
    } catch (final UnknownHostException e) {
      return new HTTPResultException<InputStream>(uri, e);
    } catch (final IOException e) {
      return new HTTPResultException<InputStream>(uri, e);
    }
  }

  private OptionType<HTTPProblemReport> getReportFromError(
    final HttpURLConnection conn)
    throws IOException {
    final OptionType<HTTPProblemReport> report;
    if (HTTP_PROBLEM_REPORT_CONTENT_TYPE.equals(conn.getContentType())) {
      if (conn.getErrorStream() != null) {
        final HTTPProblemReport r =
          HTTPProblemReport.fromStream(conn.getErrorStream());
        report = Option.some(r);
      } else {
        report = Option.none();
      }

    } else {
      report = Option.none();
    }
    return report;
  }

  /*
   * https://jira.nypl.org/browse/SIMPLY-2402: We want problem report documents to be
   * able to override the status code returned in HTTP responses in order to allow for
   * easier testing.
   */

  private StatusCodeOverride statusCodeOverrideFrom(
    final HttpURLConnection conn)
    throws IOException {
    final OptionType<HTTPProblemReport> reportOpt = getReportFromError(conn);
    if (reportOpt.isSome()) {
      final HTTPProblemReport report = ((Some<HTTPProblemReport>) reportOpt).get();
      final OptionType<Integer> problemCodeOpt = report.getProblemStatusCode();
      if (problemCodeOpt.isSome()) {
        return new StatusCodeOverride(reportOpt, problemCodeOpt);
      }
    }
    return new StatusCodeOverride(Option.none(), Option.none());
  }

  private static final class StatusCodeOverride {
    private final OptionType<HTTPProblemReport> reportOpt;
    private final OptionType<Integer> codeOpt;

    StatusCodeOverride(
      OptionType<HTTPProblemReport> reportOpt,
      OptionType<Integer> codeOpt) {
      this.reportOpt = reportOpt;
      this.codeOpt = codeOpt;
    }

    int statusCode(int existingCode) {
      return this.codeOpt.accept(
        new OptionVisitorType<Integer, Integer>() {
          @Override
          public Integer none(None<Integer> n) {
            return existingCode;
          }

          @Override
          public Integer some(Some<Integer> s) {
            return s.get();
          }
        });
    }
  }

  @Override
  public HTTPResultType<InputStream> head(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri) {
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
      conn.setReadTimeout(
        (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));

      if (auth_opt.isSome()) {
        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        auth.setConnectionParameters(conn);
      }

      conn.connect();

      int code = conn.getResponseCode();
      HTTP.LOG.trace("HEAD {} (auth {}) (result {})", uri, auth_opt, code);

      final StatusCodeOverride override = statusCodeOverrideFrom(conn);
      code = override.statusCode(code);

      if (code >= 400) {
        return new HTTPResultError<InputStream>(
          code,
          NullCheck.notNull(conn.getResponseMessage()),
          (long) conn.getContentLength(),
          NullCheck.notNull(conn.getHeaderFields()),
          conn.getLastModified(),
          this.getErrorStreamOrEmpty(conn),
          override.reportOpt);
      }

      return new HTTPResultOK<InputStream>(
        NullCheck.notNull(conn.getResponseMessage()),
        code,
        new ByteArrayInputStream(new byte[0]),
        (long) conn.getContentLength(),
        NullCheck.notNull(conn.getHeaderFields()),
        conn.getLastModified());
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final UnknownHostException e) {
      return new HTTPResultException<InputStream>(uri, e);
    } catch (final IOException e) {
      return new HTTPResultException<InputStream>(uri, e);
    }
  }

  private InputStream getErrorStreamOrEmpty(final HttpURLConnection conn) {
    final InputStream stream = conn.getErrorStream();
    if (stream != null) {
      return stream;
    }
    return new ByteArrayInputStream(new byte[0]);
  }
}
