package org.nypl.simplified.splash

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import org.nypl.simplified.books.eula.EULAType
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.*
import org.slf4j.LoggerFactory

class SplashEULAFragment : Fragment() {

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.eula"

    fun newInstance(parameters: SplashParameters): SplashEULAFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashEULAFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var agree: Button
  private lateinit var disagree: Button
  private lateinit var webView: WebView
  private lateinit var listener: SplashListenerType
  private lateinit var eula: EULAType
  private lateinit var parameters: SplashParameters
  private val logger = LoggerFactory.getLogger(SplashEULAFragment::class.java)

  override fun onCreate(state: Bundle?) {
    this.logger.debug("onCreate")
    super.onCreate(state)
    this.retainInstance = true
    this.parameters =
      this.arguments!!.getSerializable(parametersKey) as SplashParameters
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.listener = this.activity as SplashListenerType
    this.eula = this.listener.onSplashEULARequested()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {
    val view = inflater.inflate(R.layout.splash_eula, container, false)

    this.agree = view.findViewById(R.id.eula_agree)
    this.disagree = view.findViewById(R.id.eula_disagree)
    this.webView = view.findViewById(R.id.eula_web_view)

    this.agree.setOnClickListener {
      this.eula.eulaSetHasAgreed(true)
      this.listener.splashEvents.send(SplashEULAAgreed(0))
    }
    this.disagree.setOnClickListener {
      this.eula.eulaSetHasAgreed(false)
      this.listener.splashEvents.send(SplashEULADisagreed(0))
    }

    val url = this.eula.documentGetReadableURL()
    this.logger.debug("eula:     {}", this.eula)
    this.logger.debug("eula URL: {}", url)

    this.webView.settings.allowFileAccess = true
    this.webView.settings.allowContentAccess = true
    this.webView.settings.setSupportMultipleWindows(false)
    this.webView.settings.allowUniversalAccessFromFileURLs = false
    this.webView.settings.javaScriptEnabled = false
    this.webView.loadUrl(url.toString())
    return view
  }
}