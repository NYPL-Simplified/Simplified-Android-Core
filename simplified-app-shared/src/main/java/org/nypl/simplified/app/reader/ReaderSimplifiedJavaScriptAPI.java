package org.nypl.simplified.app.reader;

import android.webkit.WebView;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.utilities.UIThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link ReaderSimplifiedJavaScriptAPIType}
 * interface.
 */

public final class ReaderSimplifiedJavaScriptAPI
  implements ReaderSimplifiedJavaScriptAPIType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ReaderSimplifiedJavaScriptAPI.class);

  private final WebView web_view;

  private ReaderSimplifiedJavaScriptAPI(
    final WebView wv)
  {
    this.web_view = NullCheck.notNull(wv);
  }

  /**
   * Construct a new JavaScript API.
   *
   * @param wv The web view
   *
   * @return A new API
   */

  public static ReaderSimplifiedJavaScriptAPIType newAPI(
    final WebView wv)
  {
    return new ReaderSimplifiedJavaScriptAPI(wv);
  }

  private void evaluate(
    final String script)
  {
    ReaderSimplifiedJavaScriptAPI.LOG.debug("sending javascript: {}", script);

    final WebView wv = this.web_view;
    UIThread.runOnUIThread(
      new Runnable()
      {
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

  @Override public void getReadiumCFI()
  {
    this.evaluate("simplified.getReadiumCFI();");
  }

  @Override public void setReadiumCFI()
  {
    this.evaluate("simplified.setReadiumCFI();");
  }
}
