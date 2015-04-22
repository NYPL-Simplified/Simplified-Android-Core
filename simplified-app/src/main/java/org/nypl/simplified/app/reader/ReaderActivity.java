package org.nypl.simplified.app.reader;

import java.io.File;
import java.net.URI;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderViewerSettings.ScrollMode;
import org.nypl.simplified.app.reader.ReaderViewerSettings.SyntheticSpreadMode;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The main reader activity for reading an EPUB.
 */

public final class ReaderActivity extends Activity implements
  ReaderHTTPServerStartListenerType,
  ReaderSimplifiedFeedbackListenerType,
  ReaderReadiumFeedbackListenerType,
  ReaderReadiumEPUBLoadListenerType,
  ReaderCurrentPageListenerType
{
  private static final String FILE_ID;
  private static final String TAG = "RA";

  static {
    FILE_ID = "org.nypl.simplified.app.ReaderActivity.file";
  }

  public static void startActivity(
    final Activity from,
    final File file)
  {
    NullCheck.notNull(file);
    final Bundle b = new Bundle();
    b.putSerializable(ReaderActivity.FILE_ID, file);
    final Intent i = new Intent(from, ReaderActivity.class);
    i.putExtras(b);
    from.startActivity(i);
  }

  private @Nullable Container                      container;
  private @Nullable ReaderReadiumJavaScriptAPIType js_api;
  private @Nullable ProgressBar                    loading;
  private @Nullable ProgressBar                    progress_bar;
  private @Nullable TextView                       progress_text;
  private @Nullable ReaderViewerSettings           viewer_settings;
  private @Nullable WebView                        web_view;

  private void makeInitialReadiumRequest(
    final ReaderHTTPServerType hs)
  {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    final WebView wv = NullCheck.notNull(this.web_view);
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        Log.d(
          ReaderActivity.TAG,
          String.format("making initial reader request (%s)", reader_uri));
        wv.loadUrl(reader_uri.toString());
      }
    });
  }

  @Override public void onConfigurationChanged(
    final @Nullable Configuration c)
  {
    super.onConfigurationChanged(c);

    Log.d(ReaderActivity.TAG, "configuration changed");
    final WebView in_web_view = NullCheck.notNull(this.web_view);
    in_web_view.setVisibility(View.INVISIBLE);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.reader);

    final File epub_file = new File("/storage/sdcard0/book.epub");

    this.viewer_settings =
      new ReaderViewerSettings(
        SyntheticSpreadMode.AUTO,
        ScrollMode.AUTO,
        100,
        30);

    final ReaderReadiumFeedbackDispatcherType rd =
      ReaderReadiumFeedbackDispatcher.newDispatcher();
    final ReaderSimplifiedFeedbackDispatcherType sd =
      ReaderSimplifiedFeedbackDispatcher.newDispatcher();

    final TextView in_progress_text =
      NullCheck.notNull((TextView) this
        .findViewById(R.id.reader_position_text));
    final ProgressBar in_progress_bar =
      NullCheck.notNull((ProgressBar) this
        .findViewById(R.id.reader_position_progress));
    final ProgressBar in_loading =
      NullCheck.notNull((ProgressBar) this.findViewById(R.id.reader_loading));
    final WebView in_webview =
      NullCheck.notNull((WebView) this.findViewById(R.id.reader_webview));

    in_loading.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_webview.setVisibility(View.INVISIBLE);

    this.loading = in_loading;
    this.progress_text = in_progress_text;
    this.progress_bar = in_progress_bar;
    this.web_view = in_webview;

    in_webview.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(
        final @Nullable WebView view,
        final @Nullable String url)
      {
        final String nu = NullCheck.notNull(url);
        final URI uu = NullCheck.notNull(URI.create(nu));

        Log.d(ReaderActivity.TAG, "should-intercept: " + nu);

        if (nu.startsWith("simplified:")) {
          sd.dispatch(uu, ReaderActivity.this);
          return true;
        }

        if (nu.startsWith("readium:")) {
          rd.dispatch(uu, ReaderActivity.this);
          return true;
        }

        return super.shouldOverrideUrlLoading(view, url);
      }
    });

    final WebSettings s = NullCheck.notNull(in_webview.getSettings());
    s.setAppCacheEnabled(false);
    s.setAllowFileAccess(false);
    s.setAllowFileAccessFromFileURLs(false);
    s.setAllowContentAccess(false);
    s.setCacheMode(WebSettings.LOAD_NO_CACHE);
    s.setGeolocationEnabled(false);
    s.setJavaScriptEnabled(true);
    this.js_api = ReaderReadiumJavaScriptAPI.newAPI(in_webview);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderReadiumEPUBLoaderType pl = rs.getEPUBLoader();
    pl.loadEPUB(epub_file, this);
  }

  @Override public void onCurrentPageError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }

  @Override public void onCurrentPageReceived(
    final ReaderBookLocation l)
  {
    Log.d(ReaderActivity.TAG, "received book location: " + l);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override public void onEPUBLoadFailed(
    final Throwable x)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      "Could not load EPUB file",
      x,
      new Runnable() {
        @Override public void run()
        {
          ReaderActivity.this.finish();
        }
      });
  }

  @Override public void onEPUBLoadSucceeded(
    final Container c)
  {
    this.container = c;

    /**
     * Get a reference to the web server. Start it if necessary (the callbacks
     * will still be executed if the server is already running).
     */

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    final Package p = NullCheck.notNull(c.getDefaultPackage());
    hs.startIfNecessaryForPackage(p, this);
  }

  @Override public void onReadiumFunctionDispatchError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }

  @Override public void onReadiumFunctionInitialize()
  {
    Log.d(ReaderActivity.TAG, "readium initialize requested");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    final Container c = NullCheck.notNull(this.container);
    final Package p = NullCheck.notNull(c.getDefaultPackage());
    p.setRootUrls(hs.getURIBase().toString(), null);

    final ReaderViewerSettings vs = NullCheck.notNull(this.viewer_settings);
    final ReaderReadiumJavaScriptAPIType js = NullCheck.notNull(this.js_api);

    final OptionType<ReaderOpenPageRequest> no_request = Option.none();
    js.openBook(p, vs, no_request);

    final WebView in_web_view = NullCheck.notNull(this.web_view);
    final ProgressBar in_loading = NullCheck.notNull(this.loading);
    final ProgressBar in_progress_bar = NullCheck.notNull(this.progress_bar);
    final TextView in_progress_text = NullCheck.notNull(this.progress_text);

    in_loading.setVisibility(View.GONE);
    in_web_view.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.VISIBLE);
    in_progress_text.setVisibility(View.VISIBLE);

    Log.d(ReaderActivity.TAG, "requested openBook");
  }

  @Override public void onReadiumFunctionInitializeError(
    final Throwable e)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      "Unable to initialize Readium",
      e,
      new Runnable() {
        @Override public void run()
        {
          ReaderActivity.this.finish();
        }
      });
  }

  /**
   * {@inheritDoc}
   *
   * When the device orientation changes, the configuration change handler
   * {@link #onConfigurationChanged(Configuration)} makes the web view
   * invisible so that the user does not see the now incorrectly-paginated
   * content. When Readium tells the app that the content pagination has
   * changed, it makes the web view visible again.
   */

  @Override public void onReadiumFunctionPaginationChanged(
    final ReaderPaginationChangedEvent e)
  {
    Log.d(ReaderActivity.TAG, "pagination changed");
    final WebView in_web_view = NullCheck.notNull(this.web_view);

    final ReaderReadiumJavaScriptAPIType js = NullCheck.notNull(this.js_api);
    js.getCurrentPage(this);

    /**
     * Make the web view visible with a slight delay (as sometimes a
     * pagination-change event will be sent even though the content has not
     * yet been laid out in the web view).
     */

    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override public void run()
      {
        UIThread.runOnUIThread(new Runnable() {
          @Override public void run()
          {
            in_web_view.setVisibility(View.VISIBLE);
          }
        });
      }
    }, 200);
  }

  @Override public void onReadiumFunctionPaginationChangedError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }

  @Override public void onReadiumFunctionUnknown(
    final String text)
  {
    Log.e(
      ReaderActivity.TAG,
      String.format("unknown readium function: %s", text));
  }

  @Override protected void onResume()
  {
    super.onResume();
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);
  }

  @Override public void onServerStartFailed(
    final ReaderHTTPServerType hs,
    final Throwable x)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      "Could not start http server",
      x,
      new Runnable() {
        @Override public void run()
        {
          ReaderActivity.this.finish();
        }
      });
  }

  @Override public void onServerStartSucceeded(
    final ReaderHTTPServerType hs,
    final boolean first)
  {
    if (first) {
      Log.d(ReaderActivity.TAG, "http server started");
    } else {
      Log.d(ReaderActivity.TAG, "http server already running");
    }

    this.makeInitialReadiumRequest(hs);
  }

  @Override public void onSimplifiedFunctionDispatchError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }

  @Override public void onSimplifiedFunctionUnknown(
    final String text)
  {
    Log.e(ReaderActivity.TAG, String.format("unknown function: %s", text));
  }

  @Override public void onSimplifiedGestureLeft()
  {
    final ReaderReadiumJavaScriptAPIType js = NullCheck.notNull(this.js_api);
    js.pagePrevious();
  }

  @Override public void onSimplifiedGestureLeftError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }

  @Override public void onSimplifiedGestureRight()
  {
    final ReaderReadiumJavaScriptAPIType js = NullCheck.notNull(this.js_api);
    js.pageNext();
  }

  @Override public void onSimplifiedGestureRightError(
    final Throwable x)
  {
    Log.e(ReaderActivity.TAG, x.getMessage(), x);
  }
}
