package org.nypl.simplified.app.reader;

import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.app.utilities.TextUtilities;
import org.nypl.simplified.app.utilities.UIThread;

import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The default implementation of the {@link ReaderReadiumJavaScriptAPIType}
 * interface.
 */

public final class ReaderReadiumJavaScriptAPI implements
  ReaderReadiumJavaScriptAPIType
{
  private static final String TAG = "RRJSA";

  public static ReaderReadiumJavaScriptAPIType newAPI(
    final WebView wv)
  {
    return new ReaderReadiumJavaScriptAPI(wv);
  }

  private final WebView web_view;

  private ReaderReadiumJavaScriptAPI(
    final WebView wv)
  {
    this.web_view = NullCheck.notNull(wv);
  }

  private void evaluate(
    final String script)
  {
    Log.d(
      ReaderReadiumJavaScriptAPI.TAG,
      String.format("sending js: %s", script));

    final WebView wv = this.web_view;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        wv.evaluateJavascript(script, null);
      }
    });
  }

  private void evaluateWithResult(
    final String script,
    final ValueCallback<String> callback)
  {
    Log.d(
      ReaderReadiumJavaScriptAPI.TAG,
      String.format("sending js: %s", script));

    final WebView wv = this.web_view;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        wv.evaluateJavascript(script, callback);
      }
    });
  }

  @Override public void openBook(
    final org.readium.sdk.android.Package p,
    final ReaderViewerSettings vs,
    final OptionType<ReaderOpenPageRequest> r)
  {
    try {
      final JSONObject o = new JSONObject();
      o.put("package", p.toJSON());
      o.put("settings", vs.toJSON());

      if (r.isSome()) {
        final Some<ReaderOpenPageRequest> some =
          (Some<ReaderOpenPageRequest>) r;
        o.put("openPageRequest", some.get().toJSON());
      }

      this.evaluate(NullCheck.notNull(String.format(
        "ReadiumSDK.reader.openBook(%s)",
        o)));
    } catch (final JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override public void pageNext()
  {
    this.evaluate("ReadiumSDK.reader.openPageRight();");
  }

  @Override public void pagePrevious()
  {
    this.evaluate("ReadiumSDK.reader.openPageLeft();");
  }

  @Override public void getCurrentPage(
    final ReaderCurrentPageListenerType l)
  {
    NullCheck.notNull(l);

    this.evaluateWithResult(
      "ReadiumSDK.reader.bookmarkCurrentPage()",
      new ValueCallback<String>() {
        @Override public void onReceiveValue(
          final @Nullable String value)
        {
          try {
            final JSONObject o =
              new JSONObject(TextUtilities.unquote(NullCheck.notNull(value)));
            final ReaderBookLocation loc = ReaderBookLocation.fromJSON(o);
            l.onCurrentPageReceived(loc);
          } catch (final Throwable x) {
            try {
              l.onCurrentPageError(x);
            } catch (final Throwable x1) {
              Log.e(ReaderReadiumJavaScriptAPI.TAG, x1.getMessage(), x1);
            }
          }
        }
      });
  }
}
