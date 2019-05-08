package org.nypl.simplified.app.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.catalog.MainCatalogActivity
import org.nypl.simplified.app.profiles.ProfileSelectionActivity
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.branding.BrandingSplashServiceType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.splash.SplashEULAFragment
import org.nypl.simplified.splash.SplashEvent
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.SplashEULAAgreed
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.SplashEULADisagreed
import org.nypl.simplified.splash.SplashEvent.SplashImageEvent.SplashImageTimedOut
import org.nypl.simplified.splash.SplashImageFragment
import org.nypl.simplified.splash.SplashListenerType
import org.nypl.simplified.splash.SplashMainFragment
import org.nypl.simplified.splash.SplashParameters
import org.nypl.simplified.theme.ThemeControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class SplashActivity : AppCompatActivity(), SplashListenerType {

  override val profileController: ProfilesControllerType =
    Simplified.getProfilesController()

  override fun onSplashOpenProfileSelector() {
    val intent = Intent()
    intent.setClass(this, ProfileSelectionActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    this.startActivity(intent)
    this.finish()
  }

  override fun onSplashOpenProfileAnonymous() {
    this.profileController.profileSelect(this.profileController.profileCurrent().id())
  }

  override fun onSplashOpenCatalog(account: AccountType) {
    val intent = Intent()
    intent.setClass(this, MainCatalogActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    this.startActivity(intent)
    this.finish()
  }

  override fun onSplashEULAIsProvided(): Boolean {
    return getAvailableEULA() != null
  }

  override val splashEvents: ObservableType<SplashEvent> =
    Observable.create<SplashEvent>()

  override fun onSplashImageCreateFragment() {
    this.splashImageFragment = SplashImageFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splash_fragment_holder, this.splashImageFragment, "SPLASH_IMAGE")
      .commit()
  }

  override fun onSplashEULACreateFragment() {
    this.splashEULAFragment =
      SplashEULAFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splash_fragment_holder, this.splashEULAFragment, "SPLASH_EULA")
      .commit()
  }

  override fun onSplashEULARequested(): EULAType {
    this.log.debug("onSplashEULARequested")
    return this.getAvailableEULA()!!
  }

  private fun getAvailableEULA(): EULAType? {
    val eulaOpt = Simplified.getDocumentStore().eula
    return if (eulaOpt is Some<EULAType>) {
      eulaOpt.get()
    } else {
      null
    }
  }

  override val backgroundExecutor: ListeningScheduledExecutorService =
    Simplified.getBackgroundTaskExecutor()

  private val log: Logger = LoggerFactory.getLogger(SplashActivity::class.java)

  private lateinit var splashImageFragment: SplashImageFragment
  private lateinit var splashEULAFragment: SplashEULAFragment
  private lateinit var parameters: SplashParameters
  private lateinit var splashMainFragment: SplashMainFragment
  private var splashEventSubscription: ObservableSubscriptionType<SplashEvent>? = null

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")
    super.onCreate(null)

    this.setTheme(Simplified.getCurrentTheme().themeWithNoActionBar)
    this.setContentView(R.layout.splash_base)

    this.splashEventSubscription =
      this.splashEvents.subscribe { event ->
        when (event) {
          is SplashImageTimedOut -> Unit
          is SplashEULAAgreed -> Unit
          is SplashEULADisagreed -> this.onEulaDisagreed()
        }
      }

    /*
     * Look up and use the first available splash service.
     */

    val splashService =
      ServiceLoader.load(BrandingSplashServiceType::class.java)
        .toList()
        .firstOrNull()
        ?: throw IllegalStateException(
          "Application is misconfigured: No available services of type ${BrandingSplashServiceType::class.java.canonicalName}")

    this.log.debug("using splash service: ${splashService.javaClass.canonicalName}")

    this.parameters =
      SplashParameters(
        textColor = resources.getColor(ThemeControl.themeFallback.color),
        background = Color.WHITE,
        splashImageResource = splashService.splashImageResource(),
        splashImageSeconds = 2L)

    this.splashMainFragment =
      SplashMainFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .add(this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }

  override fun onDestroy() {
    this.log.debug("onDestroy")
    super.onDestroy()

    this.splashEventSubscription?.unsubscribe()
  }

  private fun onEulaDisagreed() {
    this.log.debug("user disagreed with EULA, shutting down!")
    this.finish()
  }
}