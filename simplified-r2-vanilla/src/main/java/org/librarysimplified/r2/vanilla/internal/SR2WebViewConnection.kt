package org.librarysimplified.r2.vanilla.internal

import android.view.MotionEvent
import android.webkit.WebView
import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2ControllerCommandQueueType
import org.nypl.simplified.ui.thread.api.UIThread

/**
 * A connection to a web view.
 */

internal class SR2WebViewConnection(
  val jsAPI: SR2JavascriptAPI,
  val webChromeClient: SR2WebChromeClient,
  val webView: WebView,
  val webViewClient: SR2WebViewClient
) {

  companion object {

    fun create(
      webView: WebView,
      jsReceiver: SR2JavascriptAPIReceiverType,
      commandQueue: SR2ControllerCommandQueueType
    ): SR2WebViewConnection {
      val webViewClient = SR2WebViewClient()
      val webChromeClient = SR2WebChromeClient()

      webView.settings.javaScriptEnabled = true
      webView.webViewClient = webViewClient
      webView.webChromeClient = webChromeClient
      webView.isVerticalScrollBarEnabled = false
      webView.isHorizontalScrollBarEnabled = false

      /*
       * Disable manual scrolling on the web view. Scrolling is controlled via the javascript API.
       */

      webView.setOnTouchListener { v, event ->
        event.action == MotionEvent.ACTION_MOVE
      }

      webView.addJavascriptInterface(jsReceiver, "Android")

      return SR2WebViewConnection(
        jsAPI = SR2JavascriptAPI(webView, commandQueue),
        webChromeClient = webChromeClient,
        webView = webView,
        webViewClient = webViewClient
      )
    }
  }

  @UiThread
  fun openURL(
    location: String,
    onLoad: () -> Unit
  ) {
    UIThread.checkIsUIThread()
    this.webViewClient.addOnLoadHandler(location, onLoad)
    this.webView.loadUrl(location)
  }
}
