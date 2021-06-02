package org.nypl.simplified.ui.catalog.saml20

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.librarysimplified.services.api.Services
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.ui.catalog.R

/**
 * A fragment that performs the SAML 2.0 borrowing login workflow.
 */

class CatalogSAML20Fragment : Fragment(R.layout.book_saml20) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment"

    /**
     * Create a new borrowing login fragment for the given parameters.
     */

    fun create(parameters: CatalogSAML20FragmentParameters): CatalogSAML20Fragment {
      val fragment = CatalogSAML20Fragment()
      fragment.arguments = bundleOf(PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val listener: FragmentListenerType<CatalogSAML20Event> by fragmentListeners()

  private val parameters: CatalogSAML20FragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as CatalogSAML20FragmentParameters
  }

  private val services = Services.serviceDirectory()

  private val viewModel: CatalogSAML20ViewModel by viewModels(
    factoryProducer = {
      CatalogSAML20ViewModelFactory(
        services = services,
        parameters = parameters,
        listener = listener,
        webViewDataDir = this.requireContext().getDir("webview", Context.MODE_PRIVATE)
      )
    }
  )

  private lateinit var progress: ProgressBar
  private lateinit var webView: WebView

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.progress = view.findViewById(R.id.saml20progressBar)
    this.webView = view.findViewById(R.id.saml20WebView)

    this.webView.webChromeClient = CatalogSAML20ChromeClient(this.progress)
    this.webView.webViewClient = this.viewModel.webViewClient
    this.webView.settings.javaScriptEnabled = true
    this.webView.setDownloadListener { _, _, _, mime, _ -> this.viewModel.downloadStarted(mime) }

    this.viewModel.webviewRequest.observe(this.viewLifecycleOwner) {
      this.webView.loadUrl(it.url, it.headers)
    }
  }
}
