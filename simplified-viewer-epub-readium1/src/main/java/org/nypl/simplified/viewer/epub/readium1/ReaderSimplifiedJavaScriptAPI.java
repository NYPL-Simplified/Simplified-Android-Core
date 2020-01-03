package org.nypl.simplified.viewer.epub.readium1;

import android.webkit.WebView;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.ui.thread.api.UIThreadServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link ReaderSimplifiedJavaScriptAPIType}
 * interface.
 */

public final class ReaderSimplifiedJavaScriptAPI
  implements ReaderSimplifiedJavaScriptAPIType
{
  private static final Logger LOG = LoggerFactory.getLogger(ReaderSimplifiedJavaScriptAPI.class);

  private final WebView web_view;
  private final UIThreadServiceType uiThread;

  private ReaderSimplifiedJavaScriptAPI(
    final UIThreadServiceType uiThread,
    final WebView wv)
  {
    this.uiThread = uiThread;
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
    final UIThreadServiceType uiThread,
    final WebView wv)
  {
    return new ReaderSimplifiedJavaScriptAPI(uiThread, wv);
  }

  private void evaluate(
    final String script)
  {
    LOG.debug("sending javascript: {}", script);
    this.uiThread.runOnUIThread(() ->{
      this.web_view.evaluateJavascript(script, null);
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
