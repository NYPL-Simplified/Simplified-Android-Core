package org.nypl.simplified.ui.splash

import android.app.Activity
import android.webkit.WebViewClient
import android.webkit.WebView
import android.content.Intent
import android.net.MailTo
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import org.slf4j.LoggerFactory

/**
 * Used to handle mailto: links in the eula.html
 */

internal class MailtoWebViewClient(val activity: Activity) : WebViewClient() {

  private val logger = LoggerFactory.getLogger(MailtoWebViewClient::class.java)

  override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
    if (url.startsWith(MailTo.MAILTO_SCHEME)) {
      val mt = MailTo.parse(url)
      val i = newEmailIntent(mt.to, mt.subject)
      if (i.resolveActivity(activity.packageManager) != null) {
        activity.startActivity(i)
        return true
      }
    }
    return false
  }

  private fun newEmailIntent(address: String, subject: String): Intent {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:") // only email apps should handle this
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    return intent
  }

  override fun onReceivedError(
    view: WebView?,
    request: WebResourceRequest?,
    error: WebResourceError?
  ) {
    super.onReceivedError(view, request, error)
    this.logger.error("onReceivedError: {} {}", request, error)
  }
}
