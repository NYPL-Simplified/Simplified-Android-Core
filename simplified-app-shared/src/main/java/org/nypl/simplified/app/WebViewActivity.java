package org.nypl.simplified.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * A mindlessly simple activity that displays a given URI in a full-screen web
 * view.
 */

public final class WebViewActivity extends SimplifiedActivity
{
  /**
   * The name used to pass URIs to the activity.
   */

  public static final String URI_KEY =
    "org.nypl.simplified.app.WebViewActivity.uri";

  /**
   * The name used to pass titles to the activity.
   */

  public static final String TITLE_KEY =
    "org.nypl.simplified.app.WebViewActivity.title";

  /**
   * The name used to pass an application part to the activity.
   */

  public static final String PART_KEY =
    "org.nypl.simplified.app.WebViewActivity.part";

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(WebViewActivity.class);
  }

  private WebView        web_view;
  private SimplifiedPart part;

  /**
   * Construct an activity.
   */

  public WebViewActivity()
  {

  }

  /**
   * Configure the given argument bundle for use in instantiating a {@link
   * WebViewActivity}.
   *
   * @param b     The argument bundle
   * @param title The title that will be displayed
   * @param uri   The URI that will be loaded
   * @param part  The application part
   */

  public static void setActivityArguments(
    final Bundle b,
    final String uri,
    final String title,
    final SimplifiedPart part)
  {
    NullCheck.notNull(b);
    NullCheck.notNull(uri);
    NullCheck.notNull(title);
    NullCheck.notNull(part);

    b.putString(WebViewActivity.URI_KEY, uri);
    b.putString(WebViewActivity.TITLE_KEY, title);
    b.putSerializable(WebViewActivity.PART_KEY, part);
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return this.part;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return false;
  }

  @Override protected void onCreate(final Bundle state)
  {
    super.onCreate(state);

    this.setContentView(R.layout.webview);

    final Intent i = NullCheck.notNull(this.getIntent());
    final String uri =
      NullCheck.notNull(i.getStringExtra(WebViewActivity.URI_KEY));
    final String title =
      NullCheck.notNull(i.getStringExtra(WebViewActivity.TITLE_KEY));

    setTitle(title);
    WebViewActivity.LOG.debug("uri: {}", uri);
    WebViewActivity.LOG.debug("title: {}", title);

    this.part = NullCheck.notNull(
      (SimplifiedPart) i.getSerializableExtra(WebViewActivity.PART_KEY));
    this.web_view =
      NullCheck.notNull((WebView) this.findViewById(R.id.web_view));

    final ActionBar bar = this.getActionBar();
    bar.setTitle(title);
    bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
    bar.setDisplayHomeAsUpEnabled(true);
    bar.setHomeButtonEnabled(true);

    final WebSettings settings = this.web_view.getSettings();
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setSupportMultipleWindows(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setJavaScriptEnabled(false);
    settings.setBlockNetworkImage(true);
    settings.setBlockNetworkLoads(true);

    this.web_view.loadUrl(uri);
  }
}
