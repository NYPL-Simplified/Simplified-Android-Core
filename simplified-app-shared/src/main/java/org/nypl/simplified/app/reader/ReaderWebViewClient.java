package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;

/**
 * The web client responsible for overriding the requests for certain
 * resources.
 */

final class ReaderWebViewClient extends WebViewClient
{
  private static final Logger LOG = LoggerFactory.getLogger(ReaderActivity.class);

  private final ReaderSimplifiedFeedbackDispatcherType simplified_dispatcher;
  private final ReaderReadiumFeedbackDispatcherType    readium_dispatcher;
  private final Activity                               activity;
  private final ReaderSimplifiedFeedbackListenerType   simplified_listener;
  private final ReaderReadiumFeedbackListenerType      readium_listener;

  public ReaderWebViewClient(
    final Activity in_activity,
    final ReaderSimplifiedFeedbackDispatcherType in_simplified_dispatcher,
    final ReaderSimplifiedFeedbackListenerType in_simplified_listener,
    final ReaderReadiumFeedbackDispatcherType in_readium_dispatcher,
    final ReaderReadiumFeedbackListenerType in_readium_listener)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.simplified_dispatcher = NullCheck.notNull(in_simplified_dispatcher);
    this.simplified_listener = NullCheck.notNull(in_simplified_listener);
    this.readium_dispatcher = NullCheck.notNull(in_readium_dispatcher);
    this.readium_listener = NullCheck.notNull(in_readium_listener);
  }

  private static @Nullable WebResourceResponse getInterceptedRequestResource(
    final String url)
  {
    if ("simplified-resource:OpenDyslexic3-Regular.ttf".equals(url)) {
      ReaderWebViewClient.LOG.debug("intercepted {} request", url);
      final InputStream stream =
        ReaderActivity.class.getResourceAsStream("OpenDyslexic3-Regular.ttf");
      if (stream != null) {
        return new WebResourceResponse("font/truetype", "UTF-8", stream);
      }

      ReaderWebViewClient.LOG.error("missing resource for {}", url);
    }

    return null;
  }

  @Override public void onLoadResource(
    final @Nullable WebView view,
    final @Nullable String url)
  {
    ReaderWebViewClient.LOG.debug("web-request: {}", url);
  }

  @Override public boolean shouldOverrideUrlLoading(
    final @Nullable WebView view,
    final @Nullable String url)
  {
    final String nu = NullCheck.notNull(url);
    final URI uu = NullCheck.notNull(URI.create(nu));

    ReaderWebViewClient.LOG.debug("should-intercept: {}", nu);

    if (nu.startsWith("simplified:")) {
      this.simplified_dispatcher.dispatch(uu, this.simplified_listener);
      return true;
    }

    if (nu.startsWith("readium:")) {
      this.readium_dispatcher.dispatch(uu, this.readium_listener);
      return true;
    }

    return super.shouldOverrideUrlLoading(view, url);
  }

  @Override public WebResourceResponse shouldInterceptRequest(
    final WebView view,
    final String url)
  {
    if (url.startsWith("simplified-resource:")) {
      final WebResourceResponse r =
        ReaderWebViewClient.getInterceptedRequestResource(url);
      if (r != null) {
        return r;
      }
    }

    return super.shouldInterceptRequest(view, url);
  }
}
