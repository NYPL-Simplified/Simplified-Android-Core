package org.nypl.simplified.tests.sandbox

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.nypl.audiobook.android.tests.sandbox.R
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.splash.SplashFragment
import org.nypl.simplified.splash.SplashListenerType
import org.nypl.simplified.splash.SplashParameters
import org.nypl.simplified.theme.ThemeControl
import java.net.URL
import java.util.concurrent.Executors

class SplashActivity : AppCompatActivity(), SplashListenerType {

  private val executor =
    MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

  private val bootEvents =
    Observable.create<BootEvent>()

  private val bootFuture =
    this.executor.submit {
      for (i in 0 until 5) {
        this.bootEvents.send(BootEvent.BootInProgress("Loading $i"))
        Thread.sleep(1000)
      }
      this.bootEvents.send(BootEvent.BootCompleted("Loaded!"))
    }

  override fun onSplashWantBootFuture(): ListenableFuture<*> {
    return this.bootFuture
  }

  override fun onSplashWantBootEvents(): ObservableReadableType<BootEvent> =
    this.bootEvents

  override fun onSplashEULAIsProvided(): Boolean {
    return false
  }

  override fun onSplashEULARequested(): EULAType {
    return object : EULAType {
      override fun eulaHasAgreed(): Boolean {
        return false
      }

      override fun documentSetLatestURL(u: URL?) {

      }

      override fun documentGetReadableURL(): URL {
        return URL("https://www.io7m.com")
      }

      override fun eulaSetHasAgreed(t: Boolean) {

      }
    }
  }

  override fun onSplashOpenProfileSelector() {

  }

  override fun onSplashOpenCatalog() {

  }

  override fun onSplashOpenProfileAnonymous() {

  }

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    return ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
  }

  private lateinit var splashMainFragment: SplashFragment
  private lateinit var parameters: SplashParameters

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setTheme(R.style.SimplifiedTheme_ActionBar_Blue)
    this.setContentView(R.layout.splash_host)

    this.parameters =
      SplashParameters(
        textColor = resources.getColor(ThemeControl.themeFallback.color),
        background = Color.WHITE,
        splashImageResource = R.drawable.sandbox,
        splashImageSeconds = 2L)

    this.splashMainFragment =
      SplashFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splashHolder, this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }

}
