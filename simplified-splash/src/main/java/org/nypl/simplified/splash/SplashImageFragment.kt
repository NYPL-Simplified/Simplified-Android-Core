package org.nypl.simplified.splash

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.nypl.simplified.splash.SplashEvent.SplashImageEvent.SplashImageTimedOut
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit.SECONDS

class SplashImageFragment : Fragment() {

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.image"

    fun newInstance(parameters: SplashParameters): SplashImageFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashImageFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreate(state: Bundle?) {
    this.logger.debug("onCreate")
    super.onCreate(state)

    this.retainInstance = true
    this.parameters =
      this.arguments!!.getSerializable(parametersKey) as SplashParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {
    val view = inflater.inflate(R.layout.splash_image, container, false)
    this.image = view.findViewById(R.id.splash_image)
    this.image.setImageResource(this.parameters.splashImageResource)
    return view
  }

  private val logger = LoggerFactory.getLogger(SplashImageFragment::class.java)
  private lateinit var parameters: SplashParameters
  private lateinit var image: ImageView
  private lateinit var listener: SplashListenerType

  override fun onActivityCreated(state: Bundle?) {
    super.onActivityCreated(state)

    this.listener = this.activity as SplashListenerType
    this.logger.debug("scheduling image timer")
    this.listener.backgroundExecutor.schedule({ this.onSplashScreenTimedOut() }, 2L, SECONDS)
  }

  private fun onSplashScreenTimedOut() {
    this.listener.splashEvents.send(SplashImageTimedOut(0))
  }
}