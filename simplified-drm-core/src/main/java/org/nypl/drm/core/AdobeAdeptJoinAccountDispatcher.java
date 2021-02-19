package org.nypl.drm.core;

import android.net.Uri;
import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p> The default implementation of the {@link
 * AdobeAdeptJoinAccountDispatcherType}
 * interface. </p>
 *
 * <p> The purpose of this class is to make use of the intercepted Join Accounts
 * Workflow form requests, and perform the requests manually. The reason for
 * this is that no currently supported WebView is capable of dealing with the
 * resulting data returned by the Adobe server. </p>
 */

public final class AdobeAdeptJoinAccountDispatcher
  implements AdobeAdeptJoinAccountDispatcherType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(AdobeAdeptJoinAccountDispatcher.class);
  }

  private final ExecutorService exec;

  private AdobeAdeptJoinAccountDispatcher(final ExecutorService in_exec)
  {
    this.exec = Objects.requireNonNull(in_exec);
  }

  /**
   * Create a new URI dispatcher.
   *
   * @param exec An executor
   *
   * @return A new dispatcher
   */

  public static AdobeAdeptJoinAccountDispatcherType newDispatcher(
    final ExecutorService exec)
  {
    return new AdobeAdeptJoinAccountDispatcher(exec);
  }

  private static void makeFormRequest(
    final JSONObject json,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
  {
    try {
      AdobeAdeptJoinAccountDispatcher.LOG.debug(
        "Parsing decoded json: {}", json);

      final URL url = new URL(Objects.requireNonNull(json.getString("url")));
      final String user = Objects.requireNonNull(json.getString("username"));
      final String password = Objects.requireNonNull(json.getString("password"));
      final String session_id = Objects.requireNonNull(json.getString("sessionId"));
      final String current_nonce =
        Objects.requireNonNull(json.getString("currentNonce"));
      final String locale = Objects.requireNonNull(json.getString("locale"));

      final Uri.Builder builder = new Uri.Builder();
      builder.appendQueryParameter("username", user);
      builder.appendQueryParameter("password", password);
      builder.appendQueryParameter("sessionId", session_id);
      builder.appendQueryParameter("currentNonce", current_nonce);
      builder.appendQueryParameter("locale", locale);
      builder.appendQueryParameter("responseType", "acsm");

      if (listener.onPreparedQuery(builder)) {
        final String query = builder.build().getEncodedQuery();
        AdobeAdeptJoinAccountDispatcher.runQuery(listener, url, query);
      }

    } catch (final Throwable e) {
      try {
        listener.onJoinAccountsException(e);
      } catch (final Throwable x) {
        AdobeAdeptJoinAccountDispatcher.LOG.error(
          "Listener raised exception: ", x);
      }
    }
  }

  private static void runQuery(
    final AdobeAdeptJoinAccountDispatcherListenerType listener,
    final URL url,
    final String query)
    throws IOException
  {
    AdobeAdeptJoinAccountDispatcher.LOG.debug(
      "making POST request to: {} with query {}", url, query);

    final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
    conn.setReadTimeout(
      (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));
    conn.setConnectTimeout(
      (int) TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS));
    conn.setRequestMethod("POST");
    conn.setDoInput(true);
    conn.setDoOutput(true);

    final OutputStream os = conn.getOutputStream();
    try {
      final BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
      try {
        writer.write(query);
        writer.flush();

      } finally {
        writer.close();
      }
    } finally {
      os.close();
    }
    conn.connect();

    final int code = conn.getResponseCode();
    AdobeAdeptJoinAccountDispatcher.LOG.debug(
      "received response code {}", Integer.valueOf(code));

    switch (code) {
      case 200: {
        final String type = conn.getContentType();

        AdobeAdeptJoinAccountDispatcher.LOG.debug(
          "received data of type {}", type);

        if ("application/vnd.adobe.adept+xml".equals(type)) {
          AdobeAdeptJoinAccountDispatcher.onACSM(conn, listener);
          return;
        }

        /*
         * The content type returned by the server for HTML pages
         * is typically something like "text/html;charset=ISO-8859-1".
         */

        if (type.startsWith("text/html")) {
          AdobeAdeptJoinAccountDispatcher.onHTMLPage(conn, listener);
          return;
        }

        /*
         * Some other type of data was received. It's unclear what, if anything,
         * can be done here.
         */

        AdobeAdeptJoinAccountDispatcher.onOtherData(conn, listener);
        return;
      }
      default: {
        AdobeAdeptJoinAccountDispatcher.onRemoteFailure(conn, listener);
        return;
      }
    }
  }

  private static void onRemoteFailure(
    final HttpsURLConnection conn,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
    throws IOException
  {
    AdobeAdeptJoinAccountDispatcher.LOG.debug(
      "onRemoteFailure: {}", conn.getResponseMessage());
  }

  private static void onOtherData(
    final HttpsURLConnection conn,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
    throws IOException
  {
    AdobeAdeptJoinAccountDispatcher.LOG.debug("onOtherData");

    final ByteArrayOutputStream bao =
      AdobeAdeptJoinAccountDispatcher.readEntireStream(conn.getInputStream());
  }

  private static void onHTMLPage(
    final HttpsURLConnection conn,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
    throws IOException
  {
    AdobeAdeptJoinAccountDispatcher.LOG.debug("onHTMLPage");

    final ByteArrayOutputStream bao =
      AdobeAdeptJoinAccountDispatcher.readEntireStream(conn.getInputStream());
    listener.onReceivedHTMLPage(bao.toString("UTF-8"));
  }

  private static ByteArrayOutputStream readEntireStream(final InputStream is)
    throws IOException
  {
    final byte[] buffer = new byte[8192];
    final ByteArrayOutputStream bao = new ByteArrayOutputStream();
    while (true) {
      final int r = is.read(buffer);
      if (r == -1) {
        break;
      }
      bao.write(buffer, 0, r);
    }
    is.close();
    return bao;
  }

  private static void onACSM(
    final HttpsURLConnection conn,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
    throws IOException
  {
    AdobeAdeptJoinAccountDispatcher.LOG.debug(
      "onACSM: Received ACSM file of size {}",
      Integer.valueOf(conn.getContentLength()));

    final ByteArrayOutputStream bao =
      AdobeAdeptJoinAccountDispatcher.readEntireStream(conn.getInputStream());
    listener.onReceivedACSM(bao.toString("UTF-8"));
  }

  @Override public Future<Void> onFormSubmit(
    final String uri,
    final AdobeAdeptJoinAccountDispatcherListenerType listener)
  {
    Objects.requireNonNull(uri);
    Objects.requireNonNull(listener);

    try {
      Assertions.checkPrecondition(
        uri.startsWith("adobe:join-form-submit/"),
        "Expected a URI starting with 'adobe:join-form-submit/' (got %s)",
        uri);

      final String data = Objects.requireNonNull(uri.substring(23));
      final String decoded =
        Objects.requireNonNull(URLDecoder.decode(data, "UTF-8"));
      final JSONObject json = new JSONObject(decoded);

      return this.exec.submit(
        new Runnable()
        {
          @Override public void run()
          {
            AdobeAdeptJoinAccountDispatcher.makeFormRequest(json, listener);
          }
        }, null);

    } catch (final JSONException e) {
      throw new UnreachableCodeException(e);
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }
}
