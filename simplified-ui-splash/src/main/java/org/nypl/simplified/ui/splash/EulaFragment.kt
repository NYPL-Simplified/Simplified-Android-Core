package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.documents.EULAType
import org.librarysimplified.services.api.Services
import org.slf4j.LoggerFactory

class EulaFragment : Fragment(R.layout.splash_eula) {

  private lateinit var agreeButton: Button
  private lateinit var disagreeButton: Button
  private lateinit var webview: WebView
  private lateinit var eula: EULAType

  private val logger = LoggerFactory.getLogger(EulaFragment::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val eula = Services.serviceDirectory()
      .requireService(DocumentStoreType::class.java)
      .eula

    this.eula = checkNotNull(eula)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    agreeButton = view.findViewById(R.id.splashEulaAgree)
    disagreeButton = view.findViewById(R.id.splashEulaDisagree)
    webview = view.findViewById(R.id.splashEulaWebView)

    with(webview.settings) {
      allowFileAccessFromFileURLs = true
      allowFileAccess = true
      allowContentAccess = true
      setSupportMultipleWindows(false)
      allowUniversalAccessFromFileURLs = false
      javaScriptEnabled = false
    }

    webview.webViewClient = MailtoWebViewClient(requireActivity())

    val url = eula.readableURL
    this.logger.debug("eula:     {}", eula)
    this.logger.debug("eula URL: {}", url)
    webview.loadUrl(url.toString())
  }

  override fun onStart() {
    super.onStart()
    agreeButton.setOnClickListener {
      eula.hasAgreed = true
      setResult()
    }
    disagreeButton.setOnClickListener {
      eula.hasAgreed = false
      setResult()
    }
  }

  private fun setResult() {
    setFragmentResult("", Bundle())
  }
}
