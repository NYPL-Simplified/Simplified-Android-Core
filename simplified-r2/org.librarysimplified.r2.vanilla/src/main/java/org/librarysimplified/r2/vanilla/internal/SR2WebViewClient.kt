package org.librarysimplified.r2.vanilla.internal

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap

/**
 * A web view client that provides logging of errors and also allows for the execution of
 * functions when pages have loaded.
 */

internal class SR2WebViewClient : WebViewClient() {

  companion object {
    val emptyResponse = WebResourceResponse(
      "text/plain",
      "utf-8",
      404,
      "Not Found",
      null,
      null
    )
  }

  private val logger =
    LoggerFactory.getLogger(SR2WebViewClient::class.java)

  private val onLoadHandlers =
    ConcurrentHashMap<String, Queue<() -> Unit>>()

  override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
    if (request != null) {
      val url = request.url?.toString() ?: ""

      if (url.endsWith("favicon.ico")) {
        return emptyResponse
      }
    }
    return super.shouldInterceptRequest(view, request)
  }

  override fun onLoadResource(
    view: WebView,
    url: String
  ) {
    this.logger.debug("onLoadResource: {}", url)
    super.onLoadResource(view, url)
  }

  override fun onPageFinished(
    view: WebView,
    url: String
  ) {
    this.logger.debug("onPageFinished: {}", url)

    /*
     * If there's a queue of load handlers for this URL, execute and remove
     * the first handler in the queue.
     */

    val handlers = this.onLoadHandlers[url]
    if (handlers != null) {
      if (!handlers.isEmpty()) {
        val handler = handlers.remove()
        this.logger.debug("executing load handler")
        handler.invoke()
      }
    }

    super.onPageFinished(view, url)
  }

  override fun onReceivedError(
    view: WebView,
    request: WebResourceRequest,
    error: WebResourceError
  ) {
    if (Build.VERSION.SDK_INT >= 23) {
      this.logger.error(
        "onReceivedError: {}: {} {}",
        request.url,
        error.errorCode,
        error.description
      )
    }
    super.onReceivedError(view, request, error)
  }

  override fun onReceivedHttpError(
    view: WebView,
    request: WebResourceRequest,
    errorResponse: WebResourceResponse
  ) {
    this.logger.error(
      "onReceivedHttpError: {}: {} {}",
      request.url,
      errorResponse.statusCode,
      errorResponse.reasonPhrase
    )
    super.onReceivedHttpError(view, request, errorResponse)
  }

  override fun onReceivedError(
    view: WebView,
    errorCode: Int,
    description: String,
    failingUrl: String
  ) {
    this.logger.error(
      "onReceivedError: {}: {} {}",
      failingUrl,
      errorCode,
      description
    )
    super.onReceivedError(view, errorCode, description, failingUrl)
  }

  /**
   * Add a function that will be called when [location] is loaded next.
   */

  fun addOnLoadHandler(
    location: String,
    onLoad: () -> Unit
  ) {
    val existingHandlers = this.onLoadHandlers[location] ?: LinkedList()
    existingHandlers.add(onLoad)
    this.onLoadHandlers[location] = existingHandlers
  }
}
