package org.nypl.simplified.splash

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class SplashFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(SplashFragment::class.java)

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.main"

    fun newInstance(parameters: SplashParameters): SplashFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var listener: SplashListenerType
  private lateinit var parameters: SplashParameters
  private lateinit var bootSubscription: ObservableSubscriptionType<BootEvent>
  private lateinit var viewsForImage: ViewsImage
  private lateinit var viewsForEULA: ViewsEULA
  private lateinit var bootFuture: ListenableFuture<*>

  private class ViewsImage(
    val container: View,
    val image: ImageView,
    val text: TextView,
    val progress: ProgressBar)

  private class ViewsEULA(
    val container: View,
    val eulaAgree: Button,
    val eulaDisagree: Button,
    val eulaWebView: WebView)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.retainInstance = true
    this.parameters = this.arguments!!.getSerializable(parametersKey) as SplashParameters
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.listener = this.activity as SplashListenerType
    this.bootFuture = this.listener.onSplashWantBootFuture()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val stackView =
      inflater.inflate(R.layout.splash_stack, container, false) as ViewGroup
    val imageView =
      inflater.inflate(R.layout.splash_image, container, false)
    val eulaView =
      inflater.inflate(R.layout.splash_eula, container, false)

    this.viewsForImage =
      ViewsImage(
        container = imageView,
        image = imageView.findViewById(R.id.splashImage),
        progress = imageView.findViewById(R.id.splashProgress),
        text = imageView.findViewById(R.id.splashText))

    this.viewsForEULA =
      ViewsEULA(
        container = eulaView,
        eulaAgree = eulaView.findViewById(R.id.splashEulaAgree),
        eulaDisagree = eulaView.findViewById(R.id.splashEulaDisagree),
        eulaWebView = eulaView.findViewById(R.id.splashEulaWebView))

    this.configureViewsForImage()

    stackView.addView(imageView)
    stackView.addView(eulaView)

    imageView.visibility = View.VISIBLE
    eulaView.visibility = View.INVISIBLE
    return stackView
  }

  private fun configureViewsForImage() {

    /*
     * Initially, only the image is shown.
     */

    this.viewsForImage.image.setImageResource(this.parameters.splashImageResource)
    this.viewsForImage.image.visibility = View.VISIBLE
    this.viewsForImage.progress.visibility = View.INVISIBLE
    this.viewsForImage.text.visibility = View.INVISIBLE
    this.viewsForImage.text.text = ""

    /*
     * Clicking the image makes the image invisible but makes the
     * progress bar visible.
     */

    this.viewsForImage.image.setOnClickListener {
      this.popImageView()
    }
  }

  private fun popImageView() {
    this.viewsForImage.progress.visibility = View.VISIBLE
    this.viewsForImage.text.visibility = View.VISIBLE
    this.viewsForImage.image.animation = AnimationUtils.loadAnimation(context, R.anim.zoom_fade)
  }

  private fun configureViewsForEULA(eula: EULAType) {
    this.viewsForEULA.eulaAgree.setOnClickListener {
      eula.eulaSetHasAgreed(true)
      this.onFinishEULASuccessfully()
    }
    this.viewsForEULA.eulaDisagree.setOnClickListener {
      eula.eulaSetHasAgreed(false)
      this.activity?.finish()
    }

    val url = eula.documentGetReadableURL()
    this.logger.debug("eula:     {}", eula)
    this.logger.debug("eula URL: {}", url)

    this.viewsForEULA.eulaWebView.settings.allowFileAccessFromFileURLs = true
    this.viewsForEULA.eulaWebView.settings.allowFileAccess = true
    this.viewsForEULA.eulaWebView.settings.allowContentAccess = true
    this.viewsForEULA.eulaWebView.settings.setSupportMultipleWindows(false)
    this.viewsForEULA.eulaWebView.settings.allowUniversalAccessFromFileURLs = false
    this.viewsForEULA.eulaWebView.settings.javaScriptEnabled = false

    this.viewsForEULA.eulaWebView.webViewClient = object : WebViewClient() {
      override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        this@SplashFragment.logger.error("onReceivedError: {} {} {}", errorCode, description, failingUrl)
      }

      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        this@SplashFragment.logger.error("onReceivedError: {}", error)
      }
    }

    this.viewsForImage.container.visibility = View.INVISIBLE
    this.viewsForEULA.container.visibility = View.VISIBLE
  }

  override fun onStart() {
    super.onStart()

    this.bootSubscription =
      this.listener.onSplashWantBootEvents()
        .subscribe { event -> this.onBootEvent(event) }

    /*
     * Subscribe to the boot future specifically so that we don't risk missing the delivery
     * of important "boot completed" or "boot failed" messages.
     */

    this.bootFuture.addListener(Runnable {
      try {
        this.bootFuture.get(1L, TimeUnit.SECONDS)
        this.onBootEvent(BootEvent.BootCompleted(""))
      } catch (e: Throwable) {
        this.onBootEvent(BootEvent.BootFailed(e.message ?: "", Exception(e)))
      }
    }, MoreExecutors.directExecutor())
  }

  override fun onStop() {
    super.onStop()
    this.bootSubscription.unsubscribe()
  }

  private fun onBootEvent(event: BootEvent) {
    this.runOnUIThread {
      this.onBootEventUI(event)
    }
  }

  @UiThread
  private fun onBootEventUI(event: BootEvent) {
    return when (event) {
      is BootEvent.BootInProgress -> {
        this.viewsForImage.text.text = event.message
      }

      is BootEvent.BootCompleted ->
        this.onBootEventCompletedUI(event.message)
      is BootEvent.BootFailed ->
        this.onBootEventFailedUI(event)
    }
  }

  @UiThread
  private fun onBootEventFailedUI(event: BootEvent.BootFailed) {
    if (this.viewsForImage.image.alpha > 0.0) {
      this.popImageView()
    }

    // XXX: We need to do better than this.
    // Print a useful message rather than a raw exception message, and allow
    // the user to do something such as submitting a report.
    this.viewsForImage.progress.isIndeterminate = false
    this.viewsForImage.progress.progress = 100
    this.viewsForImage.text.text = event.message
  }

  private fun onBootEventCompletedUI(message: String) {
    this.viewsForImage.progress.isIndeterminate = false
    this.viewsForImage.progress.progress = 100
    this.viewsForImage.text.text = message

    val eulaProvided = this.listener.onSplashEULAIsProvided()
    if (!eulaProvided) {
      this.onFinishEULASuccessfully()
      return
    }

    val eula = this.listener.onSplashEULARequested()
    if (eula.eulaHasAgreed()) {
      this.onFinishEULASuccessfully()
      return
    }

    this.configureViewsForEULA(eula)
  }

  /**
   * Either no EULA was provided, or one was provided and the user agreed to it.
   */

  private fun onFinishEULASuccessfully() {
    when (this.listener.onSplashWantProfilesMode()) {
      ANONYMOUS_PROFILE_ENABLED -> {
        this.listener.onSplashOpenProfileAnonymous()
        this.listener.onSplashOpenCatalog()
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.listener.onSplashOpenProfileSelector()
      }
    }
  }

  /**
   * Run the given Runnable on the UI thread.
   *
   * @param r The runnable
   */

  private fun runOnUIThread(f: () -> Unit) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post { f.invoke() }
  }
}