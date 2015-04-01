package org.nypl.simplified.http.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

/**
 * Default implementation of the {@link HTTPType} type.
 */

public final class HTTP implements HTTPType
{
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

  private static final class OK implements HTTPResultOKType<InputStream>
  {
    private final HttpURLConnection         conn;
    private final long                      content_length;
    private final Map<String, List<String>> headers;
    private final String                    message;
    private final int                       status;
    private final InputStream               stream;

    OK(
      final HttpURLConnection c)
      throws IOException
    {
      this.conn = NullCheck.notNull(c);
      this.message = NullCheck.notNull(this.conn.getResponseMessage());
      this.status = this.conn.getResponseCode();
      this.stream = NullCheck.notNull(this.conn.getInputStream());
      this.headers = NullCheck.notNull(this.conn.getHeaderFields());
      this.content_length = c.getContentLength();
    }

    @Override public void close()
      throws IOException
    {
      this.conn.disconnect();
      this.stream.close();
    }

    @Override public long getContentLength()
    {
      return this.content_length;
    }

    @Override public String getMessage()
    {
      return this.message;
    }

    @Override public Map<String, List<String>> getResponseHeaders()
    {
      return this.headers;
    }

    @Override public int getStatus()
    {
      return this.status;
    }

    @Override public InputStream getValue()
    {
      return this.stream;
    }

    @Override public <B, E extends Exception> B matchResult(
      final HTTPResultMatcherType<InputStream, B, E> m)
      throws E
    {
      return m.onHTTPOK(this);
    }
  }

  private static void checkURI(
    final URI uri)
  {
    NullCheck.notNull(uri);
    final String scheme = NullCheck.notNull(uri.getScheme());
    final boolean ok = "http".equals(scheme) || "https".endsWith(scheme);
    if (!ok) {
      final StringBuilder m = new StringBuilder();
      m.append("Unsupported URI scheme.\n");
      m.append("  URI scheme: ");
      m.append(scheme);
      m.append("\n");
      m.append("  Supported schemes: http https\n");
      throw new IllegalArgumentException(m.toString());
    }
  }

  public static HTTPType newHTTP()
  {
    return new HTTP();
  }

  private final String user_agent;

  private HTTP()
  {
    this.user_agent = HTTP.userAgent();
  }

  @Override public HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final long offset)
  {
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
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

      if (conn.getResponseCode() >= 400) {
        return new HTTPResultError<InputStream>(
          conn.getResponseCode(),
          conn.getResponseMessage(),
          conn.getContentLength(),
          conn.getHeaderFields());
      }

      return new OK(conn);
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<InputStream>(e);
    }
  }

  @Override public HTTPResultType<Unit> head(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri)
  {
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
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

      if (conn.getResponseCode() >= 400) {
        return new HTTPResultError<Unit>(
          conn.getResponseCode(),
          conn.getResponseMessage(),
          conn.getContentLength(),
          conn.getHeaderFields());
      }

      return new HTTPResultOK<Unit>(
        conn.getResponseMessage(),
        conn.getResponseCode(),
        Unit.unit(),
        conn.getContentLength(),
        conn.getHeaderFields());
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<Unit>(e);
    }
  }
}
