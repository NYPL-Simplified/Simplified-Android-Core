package org.nypl.simplified.app.reader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.ScrollMode;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.SyntheticSpreadMode;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.TextUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.slf4j.Logger;

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

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class ReaderReadiumJavaScriptAPI implements
  ReaderReadiumJavaScriptAPIType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderReadiumJavaScriptAPI.class);
  }

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
    ReaderReadiumJavaScriptAPI.LOG.debug("sending javascript: {}", script);

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
    ReaderReadiumJavaScriptAPI.LOG.debug("sending javascript: {}", script);

    final WebView wv = this.web_view;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        wv.evaluateJavascript(script, callback);
      }
    });
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
              ReaderReadiumJavaScriptAPI.LOG.error("{}", x1.getMessage(), x1);
            }
          }
        }
      });
  }

  @Override public void mediaOverlayNext()
  {
    this.evaluate("ReadiumSDK.reader.nextMediaOverlay();");
  }

  @Override public void mediaOverlayPrevious()
  {
    this.evaluate("ReadiumSDK.reader.previousMediaOverlay();");
  }

  @Override public void mediaOverlayToggle()
  {
    this.evaluate("ReadiumSDK.reader.toggleMediaOverlay();");
  }

  @Override public void openBook(
    final org.readium.sdk.android.Package p,
    final ReaderReadiumViewerSettings vs,
    final OptionType<ReaderOpenPageRequestType> r)
  {
    try {
      final JSONObject o = new JSONObject();
      o.put("package", p.toJSON());
      o.put("settings", vs.toJSON());

      if (r.isSome()) {
        final Some<ReaderOpenPageRequestType> some =
          (Some<ReaderOpenPageRequestType>) r;
        o.put("openPageRequest", some.get().toJSON());
      }

      this.evaluate(NullCheck.notNull(String.format(
        "ReadiumSDK.reader.openBook(%s)",
        o)));
    } catch (final JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override public void openContentURL(
    final String content_ref,
    final String source_href)
  {
    NullCheck.notNull(content_ref);
    NullCheck.notNull(source_href);

    this.evaluate(NullCheck.notNull(String.format(
      "ReadiumSDK.reader.openContentUrl('%s','%s',null)",
      content_ref,
      source_href)));
  }

  @Override public void pageNext()
  {
    this.evaluate("ReadiumSDK.reader.openPageRight();");
  }

  @Override public void pagePrevious()
  {
    this.evaluate("ReadiumSDK.reader.openPageLeft();");
  }

  @Override public void setPageStyleSettings(
    final ReaderSettingsType r)
  {
    try {
      final JSONObject decls = new JSONObject();

      final ReaderColorScheme cs = r.getColorScheme();

      final String color =
        NullCheck.notNull(String.format(
          "#%06x",
          cs.getForegroundColor() & 0xffffff));
      final String background =
        NullCheck.notNull(String.format(
          "#%06x",
          cs.getBackgroundColor() & 0xffffff));

      decls.put("color", color);
      decls.put("backgroundColor", background);

      final JSONObject o = new JSONObject();
      o.put("selector", "body");
      o.put("declarations", decls);

      final JSONArray a = new JSONArray();
      a.put(o);

      this
        .evaluate(NullCheck.notNull(String
          .format(
            "ReadiumSDK.reader.setBookStyles(%s); document.body.style.backgroundColor = \"%s\";",
            a,
            background)));

      final ReaderReadiumViewerSettings vs =
        new ReaderReadiumViewerSettings(
          SyntheticSpreadMode.AUTO,
          ScrollMode.AUTO,
          (int) r.getFontScale(),
          20);

      this.evaluate(NullCheck.notNull(String.format(
        "ReadiumSDK.reader.updateSettings(%s);",
        vs.toJSON())));

    } catch (final JSONException e) {
      ReaderReadiumJavaScriptAPI.LOG.error(
        "error constructing json: {}",
        e.getMessage(),
        e);
    }
  }

  @Override public void mediaOverlayIsAvailable(
    final ReaderMediaOverlayAvailabilityListenerType l)
  {
    NullCheck.notNull(l);

    this.evaluateWithResult(
      "ReadiumSDK.reader.isMediaOverlayAvailable()",
      new ValueCallback<String>() {
        @Override public void onReceiveValue(
          final @Nullable String value)
        {
          try {
            final boolean available = Boolean.valueOf(value).booleanValue();
            l.onMediaOverlayIsAvailable(available);
          } catch (final Throwable x) {
            try {
              l.onMediaOverlayIsAvailableError(x);
            } catch (final Throwable x1) {
              ReaderReadiumJavaScriptAPI.LOG.error("{}", x1.getMessage(), x1);
            }
          }
        }
      });
  }
}
