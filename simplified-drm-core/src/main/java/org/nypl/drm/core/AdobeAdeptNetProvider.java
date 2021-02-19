package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.jnull.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default implementation of the {@link AdobeAdeptNetProviderType}
 * interface.
 */

@SuppressWarnings("boxing") public final class AdobeAdeptNetProvider
  implements AdobeAdeptNetProviderType
{
  private static final Logger LOG;

  static {
    LOG =
      Objects.requireNonNull(LoggerFactory.getLogger(AdobeAdeptNetProvider.class));
  }

  private final String        user_agent;
  private final AtomicInteger recursion;
  private final AtomicBoolean want_cancel;

  private AdobeAdeptNetProvider(
    final String in_user_agent)
  {
    this.user_agent = Objects.requireNonNull(in_user_agent);
    this.recursion = new AtomicInteger(0);
    this.want_cancel = new AtomicBoolean(false);
  }

  /**
   * Retrieve a new instance of the net provider.
   *
   * @param in_user_agent The user agent that will be used to make requests.
   *
   * @return A new net provider
   */

  public static AdobeAdeptNetProviderType get(
    final String in_user_agent)
  {
    return new AdobeAdeptNetProvider(in_user_agent);
  }

  @Override public @Nullable AdobeAdeptStreamType newStream(
    final String method,
    final String url_text,
    final @Nullable AdobeAdeptStreamClientType client,
    final @Nullable String post_data_content_type,
    final @Nullable byte[] post_data)
  {
    Objects.requireNonNull(method);
    Objects.requireNonNull(url_text);

    AdobeAdeptNetProvider.LOG.debug(
      "newStream: {} {} {}", method, url_text, client);

    PostData pd = null;
    if (post_data != null) {
      final String ct = Objects.requireNonNull(post_data_content_type);
      AdobeAdeptNetProvider.LOG.debug(
        "received {} bytes of POST data of type {}", post_data.length, ct);
      pd = new PostData(ct, post_data);
    } else {
      AdobeAdeptNetProvider.LOG.debug("received no POST data");
    }

    try {
      final URL url = Objects.requireNonNull(new URL(url_text));
      final HttpURLConnection conn =
        Objects.requireNonNull((HttpURLConnection) url.openConnection());

      conn.setInstanceFollowRedirects(true);
      conn.setRequestMethod(method);
      conn.setDoInput(true);
      conn.setConnectTimeout(
        (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));
      conn.setReadTimeout(
        (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));
      conn.setRequestProperty("User-Agent", this.user_agent);
      conn.setRequestProperty("Accept-Encoding", "identity");

      return new Stream(conn, client, this.recursion, pd, this.want_cancel);
    } catch (final MalformedURLException e) {
      AdobeAdeptNetProvider.LOG.error("malformed uri: ", e);
      if (client != null) {
        client.onInitializationError(e);
      }
    } catch (final ProtocolException e) {
      AdobeAdeptNetProvider.LOG.error("protocol error: ", e);
      if (client != null) {
        client.onInitializationError(e);
      }
    } catch (final IOException e) {
      AdobeAdeptNetProvider.LOG.error("i/o error: ", e);
      if (client != null) {
        client.onInitializationError(e);
      }
    }

    return null;
  }

  @Override public void cancel()
  {
    AdobeAdeptNetProvider.LOG.debug("requesting download cancellation");
    this.want_cancel.compareAndSet(false, true);
  }

  private static final class PostData
  {
    private final String content_type;
    private final byte[] data;

    public PostData(
      final String in_content_type,
      final byte[] in_data)
    {
      this.content_type = Objects.requireNonNull(in_content_type);
      this.data = Objects.requireNonNull(in_data);
    }

    public String getContentType()
    {
      return this.content_type;
    }

    public byte[] getData()
    {
      return this.data;
    }
  }

  private static final class Stream implements AdobeAdeptStreamType
  {
    private static final Logger LOG_STREAM;

    static {
      LOG_STREAM = Objects.requireNonNull(LoggerFactory.getLogger(Stream.class));
    }

    private final           HttpURLConnection          conn;
    private final           AtomicInteger              recursion;
    private final @Nullable PostData                   post_data;
    private final           AtomicBoolean              want_cancel;
    private @Nullable       AdobeAdeptStreamClientType client;
    private                 int                        content_length;

    public Stream(
      final HttpURLConnection in_conn,
      final @Nullable AdobeAdeptStreamClientType in_client,
      final AtomicInteger in_recursion,
      final @Nullable PostData in_post_data,
      final AtomicBoolean in_want_cancel)
    {
      this.conn = Objects.requireNonNull(in_conn);
      this.client = in_client;
      this.recursion = Objects.requireNonNull(in_recursion);
      this.post_data = in_post_data;
      this.want_cancel = Objects.requireNonNull(in_want_cancel);

      Stream.LOG_STREAM.debug("constructed stream with client {}", in_client);
    }

    @Override public void onError(
      final String message)
    {
      final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
      c.onError("E_STREAM_ERROR: " + message);
    }

    @Override public void onRequestBytes(
      final long offset,
      final long size)
    {
      Stream.LOG_STREAM.debug("onRequestBytes: {} {}", offset, size);

      try {
        final byte[] buffer = new byte[4096];
        InputStream is = null;
        boolean eof = false;
        int w = 0;

        try {
          is = this.conn.getInputStream();

          while (eof == false) {
            if (this.want_cancel.compareAndSet(
              true, false)) {
              throw new CancelledDownloadException();
            }

            final byte[] data;
            final int r = is.read(buffer);
            if (r == -1) {
              eof = true;
              data = new byte[] {};
            } else {
              if (r == buffer.length) {
                data = buffer;
              } else {
                data = new byte[r];
                for (int index = 0; index < r; ++index) {
                  data[index] = buffer[index];
                }
              }
            }

            if (this.client != null) {
              this.client.onBytesReady((long) w, data, eof);
            }

            if (eof) {
              break;
            }
            w += r;
          }

          Assertions.checkInvariant(
            w == this.content_length,
            "Written bytes %d == %d",
            w,
            this.content_length);

        } catch (final IOException e) {
          final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
          c.onError(
            "E_STREAM_ERROR " + e.getMessage());
        } catch (final CancelledDownloadException e) {
          final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
          c.onError("E_NYPL_CANCELLED");
        } finally {
          try {
            if (is != null) {
              is.close();
            }
          } catch (final IOException e) {
            final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
            c.onError(
              "E_STREAM_ERROR " + e.getMessage());
          }
        }

      } finally {
        final int r = this.recursion.decrementAndGet();
        Stream.LOG_STREAM.debug("onRequestBytes: leaving (recursion {})", r);
      }
    }

    @Override public void onRequestInfo()
    {
      final int r = this.recursion.incrementAndGet();

      Stream.LOG_STREAM.debug(
        "onRequestInfo: connecting {} (recursion {})", this.conn.getURL(), r);

      /**
       * The Adobe library has various issues that cause infinite request
       * loops. The following statement is an attempt to catch when this
       * happens. Unfortunately, the library will also call the net provider
       * recursively during various operations such as device authorization,
       * so the recursion check cannot be a simple flag. The magic number
       * representing the upper bound was found through experimentation and
       * may change along with new Adobe code releases.
       */

      if (r > 8) {
        throw new IllegalStateException(
          "Recursion limit reached, Adobe library may be infinitely looping!");
      }

      try {
        final String method = this.conn.getRequestMethod();
        Stream.LOG_STREAM.debug("onRequestInfo: making {} request", method);

        if ("POST".equals(method)) {
          Stream.LOG_STREAM.debug("onRequestInfo: posting form data");
          final PostData post = this.post_data;
          if (post != null) {
            this.sendPostData(post);
          }
        } else {
          this.conn.connect();
        }

        Stream.LOG_STREAM.debug("onRequestInfo: waiting for response code");
        final int code = this.conn.getResponseCode();
        Stream.LOG_STREAM.debug("onRequestInfo: response code {}", code);

        if (code != HttpURLConnection.HTTP_OK) {
          final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
          c.onError(
            "E_STREAM_ERROR HTTP response code " + code);
          return;
        }

        {
          this.content_length = this.conn.getContentLength();
          Stream.LOG_STREAM.debug(
            "onRequestInfo: reporting size {}", this.content_length);
          final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
          c.onTotalLengthReady((long) this.content_length);
        }

        Stream.LOG_STREAM.debug("onRequestInfo: reporting properties");
        this.reportHeaders(this.conn.getHeaderFields());
        this.client.onPropertiesReady();

      } catch (final IOException e) {
        this.recursion.decrementAndGet();
        Stream.LOG_STREAM.error("i/o error: ", e);
        final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
        c.onError("E_STREAM_ERROR " + e.getMessage());
      } finally {
        Stream.LOG_STREAM.debug(
          "onRequestInfo: leaving (recursion {})", this.recursion.get());
      }
    }

    private void sendPostData(final PostData post)
      throws IOException
    {
      final byte[] post_bytes = post.getData();

      this.conn.setDoOutput(true);
      this.conn.setRequestProperty(
        "Content-Length", Integer.toString(post_bytes.length));
      this.conn.setRequestProperty(
        "Content-Type", post.getContentType());

      final OutputStream out = this.conn.getOutputStream();
      out.write(post_bytes, 0, post_bytes.length);
      out.flush();
      out.close();
    }

    private void reportHeaders(final Map<String, List<String>> headers)
    {
      for (final String k : headers.keySet()) {
        if (k == null) {
          // A null key indicates that this header is the HTTP status line
          continue;
        }

        if ("content-length".equalsIgnoreCase(k)) {
          Stream.LOG_STREAM.debug("onRequestInfo: not reporting {}", k);
          continue;
        }

        final List<String> values = Objects.requireNonNull(headers.get(k));
        for (int index = 0; index < values.size(); ++index) {
          final String value = Objects.requireNonNull(values.get(index));

          /**
           * The reason for this repeated fetching and checking of the client
           * is because we have absolutely no idea when and where the Adobe
           * library will decide to set a new (possibly null) stream client.
           *
           * We're almost certain it won't do it here, but if it does, we want
           * to know about it immediately.
           */

          final AdobeAdeptStreamClientType c = Objects.requireNonNull(this.client);
          c.onPropertyReady(k, value);
        }
      }
    }

    @Override public void onRelease()
    {
      Stream.LOG_STREAM.debug("onRelease: {}", this.conn.getURL());
      this.conn.disconnect();
    }

    @Override public void onSetStreamClient(
      final @Nullable AdobeAdeptStreamClientType c)
    {
      Stream.LOG_STREAM.debug("setStreamClient: {}", c);
      this.client = c;
    }
  }

  private static final class CancelledDownloadException extends Exception
  {
    CancelledDownloadException()
    {
      super();
    }
  }
}
