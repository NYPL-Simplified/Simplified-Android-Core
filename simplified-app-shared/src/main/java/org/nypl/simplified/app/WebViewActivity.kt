package org.nypl.simplified.app

import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Objects

/**
 * A mindlessly simple activity that displays a given URI in a full-screen web
 * view.
 */

class WebViewActivity : NavigationDrawerActivity() {

  private val logger: Logger = LoggerFactory.getLogger(WebViewActivity::class.java)
  private lateinit var webView: WebView
  private lateinit var title: String
  private lateinit var uri: String

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        this.onBackPressed()
        true
      }

      else -> {
        super.onOptionsItemSelected(item)
      }
    }
  }

  override fun navigationDrawerShouldShowIndicator(): Boolean {
    return false
  }

  override fun navigationDrawerGetActivityTitle(resources: Resources): String? {
    return this.title
  }

  override fun onCreate(state: Bundle?) {
    this.uri = intent.getStringExtra(URI_KEY)
    this.title = intent.getStringExtra(TITLE_KEY)

    super.onCreate(state)

    this.setContentView(R.layout.webview)

    this.setTitle(this.title)
    this.logger.debug("uri: {}", uri)
    this.logger.debug("title: {}", this.title)
    this.webView = Objects.requireNonNull(this.findViewById(R.id.web_view))

    val bar = this.supportActionBar
    if (bar != null) {
      bar.title = this.title
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
      bar.setDisplayHomeAsUpEnabled(true)
      bar.setHomeButtonEnabled(false)
    }

    val settings = this.webView.settings
    settings.allowFileAccess = true
    settings.allowContentAccess = true
    settings.setSupportMultipleWindows(false)
    settings.allowUniversalAccessFromFileURLs = false
    settings.javaScriptEnabled = false

    this.webView.loadUrl(uri)
  }

  companion object {

    /**
     * The name used to pass URIs to the activity.
     */

    const val URI_KEY = "org.nypl.simplified.app.WebViewActivity.uri"

    /**
     * The name used to pass titles to the activity.
     */

    const val TITLE_KEY = "org.nypl.simplified.app.WebViewActivity.title"

    /**
     * Configure the given argument bundle for use in instantiating a [ ].
     *
     * @param arguments     The argument bundle
     * @param title The title that will be displayed
     * @param uri   The URI that will be loaded
     */

    fun setActivityArguments(
      arguments: Bundle,
      uri: String,
      title: String) {
      arguments.putString(URI_KEY, uri)
      arguments.putString(TITLE_KEY, title)
    }
  }
}
