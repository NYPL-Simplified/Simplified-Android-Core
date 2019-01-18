package org.nypl.simplified.app;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mindlessly simple activity that displays a given URI in a full-screen web
 * view.
 */

public final class WebViewActivity extends NavigationDrawerActivity
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
    LOG = LoggerFactory.getLogger(WebViewActivity.class);
  }

  private WebView        web_view;
  private SimplifiedPart part;
  private String title;

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

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return false;
  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return this.title;
  }

  @Override protected void onCreate(final Bundle state)
  {
    super.onCreate(state);

    this.setContentView(R.layout.webview);

    final Intent i = NullCheck.notNull(this.getIntent());
    final String uri =
      NullCheck.notNull(i.getStringExtra(WebViewActivity.URI_KEY));
    this.title =
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
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    }
    else
    {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }

    final WebSettings settings = this.web_view.getSettings();
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setSupportMultipleWindows(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setJavaScriptEnabled(false);

    this.web_view.loadUrl(uri);
  }
}
