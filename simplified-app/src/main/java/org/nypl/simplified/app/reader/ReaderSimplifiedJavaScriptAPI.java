package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.slf4j.Logger;

import android.webkit.WebView;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link ReaderSimplifiedJavaScriptAPIType}
 * interface.
 */

public final class ReaderSimplifiedJavaScriptAPI implements
  ReaderSimplifiedJavaScriptAPIType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderSimplifiedJavaScriptAPI.class);
  }

  public static ReaderSimplifiedJavaScriptAPIType newAPI(
    final WebView wv)
  {
    return new ReaderSimplifiedJavaScriptAPI(wv);
  }

  private final WebView web_view;

  private ReaderSimplifiedJavaScriptAPI(
    final WebView wv)
  {
    this.web_view = NullCheck.notNull(wv);
  }

  private void evaluate(
    final String script)
  {
    ReaderSimplifiedJavaScriptAPI.LOG.debug("sending javascript: {}", script);

    final WebView wv = this.web_view;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        wv.evaluateJavascript(script, null);
      }
    });
  }

  @Override public void pageHasChanged()
  {
    this.evaluate("simplified.pageDidChange();");
  }
}
