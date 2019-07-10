package org.nypl.simplified.app.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.common.util.concurrent.ListenableFuture
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.SimplifiedActivity
import org.nypl.simplified.app.catalog.MainCatalogActivity
import org.nypl.simplified.app.profiles.ProfileSelectionActivity
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.branding.BrandingSplashServiceType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.splash.SplashFragment
import org.nypl.simplified.splash.SplashListenerType
import org.nypl.simplified.splash.SplashParameters
import org.nypl.simplified.theme.ThemeControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

class SplashActivity : SimplifiedActivity(), SplashListenerType {

  override fun onSplashOpenProfileAnonymous() {
    this.log.debug("onSplashOpenProfileAnonymous")

    val profilesController =
      Simplified.application.services()
        .profilesController
    profilesController.profileSelect(profilesController.profileCurrent().id)
  }

  override fun onSplashWantBootFuture(): ListenableFuture<*> {
    this.log.debug("onSplashWantBootFuture")
    return Simplified.application.servicesBooting
  }

  override fun onSplashWantBootEvents(): ObservableReadableType<BootEvent> {
    this.log.debug("onSplashWantBootEvents")
    return Simplified.application.servicesBootEvents
  }

  override fun onSplashOpenCatalog() {
    this.log.debug("onSplashOpenCatalog")

    val intent = Intent()
    intent.setClass(this, MainCatalogActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    this.startActivity(intent)
    this.finish()
  }

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    this.log.debug("onSplashWantProfilesMode")

    return Simplified.application.services().profilesController.profileAnonymousEnabled()
  }

  override fun onSplashOpenProfileSelector() {
    this.log.debug("onSplashOpenProfileSelector")

    val intent = Intent()
    intent.setClass(this, ProfileSelectionActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    this.startActivity(intent)
    this.finish()
  }

  override fun onSplashEULAIsProvided(): Boolean {
    this.log.debug("onSplashEULAIsProvided")

    return getAvailableEULA() != null
  }


  override fun onSplashEULARequested(): EULAType {
    this.log.debug("onSplashEULARequested")

    return this.getAvailableEULA()!!
  }

  private fun getAvailableEULA(): EULAType? {
    val eulaOpt = Simplified.application.services().documentStore.eula
    return if (eulaOpt is Some<EULAType>) {
      eulaOpt.get()
    } else {
      null
    }
  }

  private val log: Logger = LoggerFactory.getLogger(SplashActivity::class.java)

  private lateinit var parameters: SplashParameters
  private lateinit var splashMainFragment: SplashFragment

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")
    super.onCreate(null)

    this.setTheme(R.style.SimplifiedBlank)
    this.setContentView(R.layout.splash_base)

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
      SplashFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splash_fragment_holder, this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }

  private fun onEulaDisagreed() {
    this.log.debug("user disagreed with EULA, shutting down!")
    this.finish()
  }
}