package org.nypl.simplified.app.reader;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;
import org.nypl.simplified.app.reader.ReaderViewerSettings.ScrollMode;
import org.nypl.simplified.app.reader.ReaderViewerSettings.SyntheticSpreadMode;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
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

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class ReaderActivity extends
  Activity implements
  ReaderHTTPServerStartListenerType,
  ReaderSimplifiedFeedbackListenerType,
  ReaderReadiumFeedbackListenerType,
  ReaderReadiumEPUBLoadListenerType,
  ReaderCurrentPageListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderActivity.class);
  }

  private static final String FILE_ID;

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

  private @Nullable Container                         container;
  private @Nullable ReaderReadiumJavaScriptAPIType    readium_js_api;
  private @Nullable ReaderSimplifiedJavaScriptAPIType simplified_js_api;
  private @Nullable ProgressBar                       loading;
  private @Nullable ProgressBar                       progress_bar;
  private @Nullable TextView                          progress_text;
  private @Nullable ReaderViewerSettings              viewer_settings;
  private @Nullable WebView                           web_view;
  private boolean                                     orientation_changing;

  private void makeInitialReadiumRequest(
    final ReaderHTTPServerType hs)
  {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    final WebView wv = NullCheck.notNull(this.web_view);
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        ReaderActivity.LOG.debug(
          "making initial reader request: {}",
          reader_uri);
        wv.loadUrl(reader_uri.toString());
      }
    });
  }

  @Override public void onConfigurationChanged(
    final @Nullable Configuration c)
  {
    super.onConfigurationChanged(c);

    ReaderActivity.LOG.debug("configuration changed");
    final WebView in_web_view = NullCheck.notNull(this.web_view);
    in_web_view.setVisibility(View.INVISIBLE);

    this.orientation_changing = true;
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.reader);

    final ActionBar bar = NullCheck.notNull(this.getActionBar());
    bar.hide();

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

        ReaderActivity.LOG.debug("should-intercept: {}", nu);

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

    this.readium_js_api = ReaderReadiumJavaScriptAPI.newAPI(in_webview);
    this.simplified_js_api = ReaderSimplifiedJavaScriptAPI.newAPI(in_webview);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderReadiumEPUBLoaderType pl = rs.getEPUBLoader();
    pl.loadEPUB(epub_file, this);
  }

  @Override public void onCurrentPageError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onCurrentPageReceived(
    final ReaderBookLocation l)
  {
    ReaderActivity.LOG.debug("received book location: {}", l);
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
      ReaderActivity.LOG,
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
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onReadiumFunctionInitialize()
  {
    ReaderActivity.LOG.debug("readium initialize requested");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    final Container c = NullCheck.notNull(this.container);
    final Package p = NullCheck.notNull(c.getDefaultPackage());
    p.setRootUrls(hs.getURIBase().toString(), null);

    final ReaderViewerSettings vs = NullCheck.notNull(this.viewer_settings);
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);

    final OptionType<ReaderOpenPageRequest> no_request = Option.none();
    js.openBook(p, vs, no_request);

    final WebView in_web_view = NullCheck.notNull(this.web_view);
    final ProgressBar in_loading = NullCheck.notNull(this.loading);
    final ProgressBar in_progress_bar = NullCheck.notNull(this.progress_bar);
    final TextView in_progress_text = NullCheck.notNull(this.progress_text);

    in_loading.setVisibility(View.GONE);
    in_web_view.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.VISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);

    ReaderActivity.LOG.debug("requested openBook");
  }

  @Override public void onReadiumFunctionInitializeError(
    final Throwable e)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      ReaderActivity.LOG,
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
    ReaderActivity.LOG.debug("pagination changed: {}", e);
    final WebView in_web_view = NullCheck.notNull(this.web_view);

    /**
     * Ask for Readium to deliver the unique identifier of the current page,
     * and tell Simplified that the page has changed and so any Javascript
     * state should be reconfigured.
     */

    final ReaderReadiumJavaScriptAPIType readium_js =
      NullCheck.notNull(this.readium_js_api);
    final ReaderSimplifiedJavaScriptAPIType simplified_js =
      NullCheck.notNull(this.simplified_js_api);

    readium_js.getCurrentPage(this);
    simplified_js.pageHasChanged();

    /**
     * Configure the progress bar and text.
     */

    final TextView in_progress_text = NullCheck.notNull(this.progress_text);
    final ProgressBar in_progress_bar = NullCheck.notNull(this.progress_bar);
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final double p = e.getProgressFractional();
        in_progress_bar.setMax(100);
        in_progress_bar.setProgress((int) (100 * p));

        final List<OpenPage> pages = e.getOpenPages();
        if (pages.isEmpty()) {
          in_progress_text.setVisibility(View.INVISIBLE);
        } else {
          final OpenPage page = NullCheck.notNull(pages.get(0));
          in_progress_text.setVisibility(View.VISIBLE);
          in_progress_text.setText(NullCheck.notNull(String.format(
            "Page %d of %d",
            page.getSpineItemPageIndex() + 1,
            page.getSpineItemPageCount())));
        }
      }
    });

    /**
     * Make the web view visible with a slight delay (as sometimes a
     * pagination-change event will be sent even though the content has not
     * yet been laid out in the web view). Only do this if the screen
     * orientation has just changed.
     */

    if (this.orientation_changing) {
      this.orientation_changing = false;

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
  }

  @Override public void onReadiumFunctionPaginationChangedError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onReadiumFunctionUnknown(
    final String text)
  {
    ReaderActivity.LOG.error("unknown readium function: {}", text);
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
      ReaderActivity.LOG,
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
      ReaderActivity.LOG.debug("http server started");
    } else {
      ReaderActivity.LOG.debug("http server already running");
    }

    this.makeInitialReadiumRequest(hs);
  }

  @Override public void onSimplifiedFunctionDispatchError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedFunctionUnknown(
    final String text)
  {
    ReaderActivity.LOG.error("unknown function: {}", text);
  }

  @Override public void onSimplifiedGestureLeft()
  {
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);
    js.pagePrevious();
  }

  @Override public void onSimplifiedGestureLeftError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedGestureRight()
  {
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);
    js.pageNext();
  }

  @Override public void onSimplifiedGestureRightError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedGestureCenter()
  {
    final ActionBar bar = NullCheck.notNull(this.getActionBar());
    if (bar.isShowing()) {
      bar.hide();
    } else {
      bar.show();
    }
  }

  @Override public void onSimplifiedGestureCenterError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }
}
