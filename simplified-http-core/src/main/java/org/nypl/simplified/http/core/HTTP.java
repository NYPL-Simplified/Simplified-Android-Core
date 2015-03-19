package org.nypl.simplified.http.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

/**
 * Default implementation of the {@link HTTPType} type.
 */

public final class HTTP implements HTTPType
{
  private static final class OK implements HTTPResultOKType<InputStream>
  {
    private final HttpURLConnection conn;
    private final String            message;
    private final int               status;
    private final InputStream       stream;

    OK(
      final HttpURLConnection c)
      throws IOException
    {
      this.conn = NullCheck.notNull(c);
      this.message = NullCheck.notNull(this.conn.getResponseMessage());
      this.status = this.conn.getResponseCode();
      this.stream = NullCheck.notNull(this.conn.getInputStream());
    }

    @Override public void close()
      throws IOException
    {
      this.conn.disconnect();
      this.stream.close();
    }

    @Override public String getMessage()
    {
      return this.message;
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

  private HTTP()
  {
    // Nothing
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

      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Range", "bytes=" + offset + "-");
      conn.connect();

      if (auth_opt.isSome()) {
        final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
        final HTTPAuthType auth = some.get();
        auth.setConnectionParameters(conn);
      }

      conn.connect();

      if (conn.getResponseCode() >= 400) {
        return new HTTPResultError<InputStream>(
          conn.getResponseCode(),
          conn.getResponseMessage());
      }

      return new OK(conn);
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<InputStream>(e);
    }
  }

  @Override public HTTPResultType<Long> getContentLength(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri)
  {
    NullCheck.notNull(auth_opt);
    HTTP.checkURI(uri);

    try {
      final URL url = NullCheck.notNull(uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      try {
        conn.setRequestMethod("HEAD");
        conn.setReadTimeout(10000);

        if (auth_opt.isSome()) {
          final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
          final HTTPAuthType auth = some.get();
          auth.setConnectionParameters(conn);
        }

        conn.connect();

        if (conn.getResponseCode() >= 400) {
          return new HTTPResultError<Long>(
            conn.getResponseCode(),
            conn.getResponseMessage());
        }

        return new HTTPResultOK<Long>(
          conn.getResponseMessage(),
          conn.getResponseCode(),
          Long.valueOf(conn.getContentLength()));

      } finally {
        conn.disconnect();
      }

    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    } catch (final IOException e) {
      return new HTTPResultException<Long>(e);
    }
  }
}
