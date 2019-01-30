package org.nypl.simplified.splash

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import org.nypl.simplified.books.profiles.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.books.profiles.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.SplashEULAAgreed
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.SplashEULADisagreed
import org.nypl.simplified.splash.SplashEvent.SplashImageEvent.SplashImageTimedOut
import org.slf4j.LoggerFactory

class SplashMainFragment : Fragment() {

  private val log = LoggerFactory.getLogger(SplashMainFragment::class.java)

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.main"

    fun newInstance(parameters: SplashParameters): SplashMainFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashMainFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var listener: SplashListenerType
  private lateinit var parameters: SplashParameters
  private lateinit var subscription: ObservableSubscriptionType<SplashEvent>

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.retainInstance = true
    this.parameters =
      this.arguments!!.getSerializable(SplashMainFragment.parametersKey) as SplashParameters
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.listener = this.activity as SplashListenerType
    this.subscription = this.listener.splashEvents.subscribe { event -> this.onSplashEvent(event) }
    this.listener.onSplashImageCreateFragment()
  }

  private fun onSplashImageTimedOut() {
    this.log.debug("splash image timed out")

    if (this.listener.onSplashEULAIsProvided()) {
      val eula = this.listener.onSplashEULARequested()
      if (eula.eulaHasAgreed()) {
        this.listener.splashEvents.send(SplashEULAAgreed(0))
      } else {
        this.listener.onSplashEULACreateFragment()
      }
    } else {
      this.log.debug("no EULA provided")
      this.listener.splashEvents.send(SplashEULAAgreed(0))
    }
  }

  private fun onSplashEvent(event: SplashEvent) {
    return when (event) {
      is SplashImageTimedOut ->
        this.onSplashImageTimedOut()
      is SplashEULAAgreed ->
        this.onSplashEULAAgreed()
      is SplashEULADisagreed ->
        this.onSplashEULADisagreed()
    }
  }

  private fun onSplashEULADisagreed() {
    this.log.debug("user disagreed with EULA")
  }

  private fun onSplashEULAAgreed() {
    this.log.debug("user agreed with EULA")

    val anonymous =
      this.listener.profileController.profileAnonymousEnabled()!!

    return when (anonymous) {
      ANONYMOUS_PROFILE_ENABLED -> {
        this.log.debug("anonymous profiles enabled, opening the catalog")
        this.listener.onSplashOpenProfileAnonymous()
        this.listener.onSplashOpenCatalog(this.listener.profileController.profileAccountCurrent())
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.log.debug("anonymous profiles disabled, opening the profiles screen")
        this.listener.onSplashOpenProfileSelector()
      }
    }
  }
}