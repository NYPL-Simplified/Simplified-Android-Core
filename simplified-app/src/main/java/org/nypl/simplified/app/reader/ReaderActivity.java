package org.nypl.simplified.app.reader;

import java.io.ByteArrayInputStream;
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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderActivity extends Activity implements
  ReaderHTTPServerStartListenerType,
  ReaderSimplifiedFeedbackListenerType,
  ReaderReadiumFeedbackListenerType,
  ReaderReadiumEPUBLoadListenerType
{
  private static final String FILE_ID;
  private static final String TAG = "RA";

  static {
    FILE_ID = "org.nypl.simplified.app.ReaderActivity.file";
  }

  private static WebResourceResponse emptyResponse()
  {
    return new WebResourceResponse(
      "text/plain",
      "UTF-8",
      new ByteArrayInputStream(new byte[0]));
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
  private @Nullable ReaderViewerSettings           viewer_settings;
  private @Nullable WebView                        web_view;
  private @Nullable ProgressBar                    loading;

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

    final ProgressBar pb =
      NullCheck.notNull((ProgressBar) this.findViewById(R.id.reader_loading));
    pb.setVisibility(View.VISIBLE);
    this.loading = pb;

    final WebView wv =
      NullCheck.notNull((WebView) this.findViewById(R.id.reader_webview));
    wv.setVisibility(View.INVISIBLE);
    wv.setWebViewClient(new WebViewClient() {
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

    final WebSettings s = NullCheck.notNull(wv.getSettings());
    s.setAppCacheEnabled(false);
    s.setAllowFileAccess(false);
    s.setAllowFileAccessFromFileURLs(false);
    s.setAllowContentAccess(false);
    s.setCacheMode(WebSettings.LOAD_NO_CACHE);
    s.setGeolocationEnabled(false);
    s.setJavaScriptEnabled(true);
    this.web_view = wv;
    this.js_api = ReaderReadiumJavaScriptAPI.newAPI(wv);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderReadiumEPUBLoaderType pl = rs.getEPUBLoader();
    pl.loadEPUB(epub_file, this);
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

    final WebView wv = NullCheck.notNull(this.web_view);
    wv.setVisibility(View.VISIBLE);
    final ProgressBar pb = NullCheck.notNull(this.loading);
    pb.setVisibility(View.GONE);

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

  @Override public void onSimplifiedUnknownFunction(
    final String text)
  {
    Log.e(ReaderActivity.TAG, String.format("unknown function: %s", text));
  }
}
