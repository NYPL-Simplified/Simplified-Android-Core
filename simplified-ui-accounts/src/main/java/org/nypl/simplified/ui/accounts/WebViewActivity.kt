package org.nypl.simplified.ui.accounts

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.ui.accounts.databinding.ActivityWebviewBinding
import org.slf4j.LoggerFactory

/**
 * Shows the different outbound account links as WebViews
 */
class WebViewActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(WebViewActivity::class.java)

  companion object {
    const val PAGE = "page"
    const val FAQ = "faq"
    const val LOGIN_TROUBLE = "login_trouble"
    const val PRIVACY_NOTICE = "privacy_notice"
    const val TERMS_OF_USE = "terms_of_use"
  }

  private lateinit var binding: ActivityWebviewBinding

  private val privacyNotice = "https://www.openebooks.org/policies"
  private val loginTrouble = "https://www.openebooks.org/signin-help"
  private val termsOfUse = "http://www.librarysimplified.org/EULA.html"
  private val default = "https://www.openebooks.org"
  private val faq = "https://www.openebooks.org/faq"
  private lateinit var url: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityWebviewBinding.inflate(layoutInflater)
    setContentView(binding.root)

    /**
     * Determine page to load
     */
    url = when (intent.getStringExtra(PAGE)) {
      FAQ -> faq
      LOGIN_TROUBLE -> loginTrouble
      PRIVACY_NOTICE -> privacyNotice
      TERMS_OF_USE -> termsOfUse
      else -> default
    }

    /**
     * Load URL
     */
    binding.page.loadUrl(url)
    binding.page.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView, url: String) {
        logger.debug("Web page loaded")
        binding.loading.visibility = View.GONE
      }
    }

    /**
     * Handle back button click
     */
    binding.back.setOnClickListener {
      finish()
    }
  }
}
