package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An activity that shows a license agreement, and aborts if the user does not
 * agree to it.
 */

public final class MainEULAActivity extends Activity
{

  /**
   *
   */
  public static final String URI_KEY =
    "org.nypl.simplified.app.MainEULAActivity.uri";

  /**
   * Construct an activity.
   */

  public MainEULAActivity()
  {

  }

  /**
   * @param b Bundle
   * @param uri URI
   */
  public static void setActivityArguments(
    final Bundle b,
    final String uri)
  {
    NullCheck.notNull(b);
    NullCheck.notNull(uri);

    b.putString(MainEULAActivity.URI_KEY, uri);
  }

  @Override protected void onCreate(final Bundle state)
  {
    final int id = Simplified.getCurrentAccount().getId();
    if (id == 0) {
      setTheme(R.style.SimplifiedTheme_NYPL);
    }
    else if (id == 1) {
      setTheme(R.style.SimplifiedTheme_BPL);
    }
    else if (id == 9) {
      setTheme(R.style.SimplifiedTheme_MCPL);
    }
    else {
      setTheme(R.style.SimplifiedTheme);
    }

    super.onCreate(state);

    final ActionBar bar = this.getActionBar();
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    } else {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }

    this.setContentView(R.layout.eula);


    final Intent i = NullCheck.notNull(this.getIntent());
    final String uri =
      i.getStringExtra(MainEULAActivity.URI_KEY);


    final WebView web_view = NullCheck.notNull((WebView) this.findViewById(R.id.eula_web_view));

    final WebSettings settings = web_view.getSettings();
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setSupportMultipleWindows(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setJavaScriptEnabled(false);

    if (uri != null)
    {
      web_view.loadUrl(uri);
    }
    else {
      web_view.loadUrl("http://www.librarysimplified.org/EULA.html");
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    this.overridePendingTransition(0, 0);
  }


  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);

    switch (item.getItemId()) {

      case android.R.id.home: {
        this.onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }
}
