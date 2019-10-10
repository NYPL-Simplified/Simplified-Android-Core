package org.nypl.simplified.app.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.SimplifiedActivity
import org.nypl.simplified.app.catalog.MainCatalogActivity
import org.nypl.simplified.app.profiles.ProfileSelectionActivity
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.migration.api.Migrations
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.splash.SplashFragment
import org.nypl.simplified.ui.splash.SplashListenerType
import org.nypl.simplified.ui.splash.SplashParameters
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit

class SplashActivity : SimplifiedActivity(), SplashListenerType {

  override fun onSplashDone() {
    when (Simplified.application.services().profilesController.profileAnonymousEnabled()) {
      null,
      ANONYMOUS_PROFILE_ENABLED -> {
        val intent = Intent()
        intent.setClass(this, MainCatalogActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(intent)
        this.finish()
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        val intent = Intent()
        intent.setClass(this, ProfileSelectionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(intent)
        this.finish()
      }
    }
  }

  override fun onSplashWantMigrations(): MigrationsType {

    val profilesController =
      Simplified.application.services().profilesController

    val migrationServiceDependencies =
      MigrationServiceDependencies(
        createAccount = { uri ->
          this.doCreateAccount(profilesController, uri)
        },
        loginAccount = { account, credentials ->
          this.doLoginAccount(profilesController, account, credentials)
        },
        accountEvents = profilesController.accountEvents(),
        applicationProfileIsAnonymous =
        profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_ENABLED,
        applicationVersion = this.applicationVersion(),
        context = this)

    return Migrations.create(migrationServiceDependencies)
  }

  private fun doLoginAccount(
    profilesController: ProfilesControllerType,
    account: AccountType,
    credentials: AccountAuthenticationCredentials
  ): TaskResult<AccountLoginState.AccountLoginErrorData, Unit> {
    this.log.debug("doLoginAccount")
    return profilesController.profileAccountLogin(account.id, credentials)
      .get(3L, TimeUnit.MINUTES)
  }

  private fun doCreateAccount(
    profilesController: ProfilesControllerType,
    provider: URI
  ): TaskResult<AccountCreateErrorDetails, AccountType> {
    this.log.debug("doCreateAccount")
    return profilesController.profileAccountCreateOrReturnExisting(provider)
      .get(3L, TimeUnit.MINUTES)
  }

  private fun applicationVersion(): String {
    return try {
      val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
      "${packageInfo.packageName} ${packageInfo.versionName} (${packageInfo.versionCode})"
    } catch (e: Exception) {
      this.log.error("could not get package info: ", e)
      "unknown"
    }
  }

  override fun onSplashWantMigrationExecutor(): ListeningExecutorService {
    return this.migrationExecutor
  }

  override fun onSplashMigrationReport(report: MigrationReport) {
    this.log.debug("onSplashMigrationReport")
  }

  override fun onSplashOpenProfileAnonymous() {
    this.log.debug("onSplashOpenProfileAnonymous")

    val profilesController =
      Simplified.application.services().profilesController

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

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    this.log.debug("onSplashWantProfilesMode")

    return Simplified.application.services().profilesController.profileAnonymousEnabled()
  }

  override fun onSplashEULAIsProvided(): Boolean {
    this.log.debug("onSplashEULAIsProvided")
    return this.getAvailableEULA() != null
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

  private lateinit var migrationExecutor: ListeningScheduledExecutorService
  private lateinit var parameters: SplashParameters
  private lateinit var splashMainFragment: SplashFragment

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")
    super.onCreate(null)

    this.setTheme(R.style.SimplifiedBlank)
    this.setContentView(R.layout.splash_base)

    this.migrationExecutor =
      NamedThreadPools.namedThreadPool(1, "migrations", 19)

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

    val migrationReportingEmail =
      this.resources.getString(R.string.feature_migration_report_email)
        .trim()
        .let { text -> if (text.isEmpty()) null else text }

    this.parameters =
      SplashParameters(
        textColor = this.resources.getColor(ThemeControl.themeFallback.color),
        background = Color.WHITE,
        splashMigrationReportEmail = migrationReportingEmail,
        splashImageResource = splashService.splashImageResource(),
        splashImageSeconds = 2L)

    this.splashMainFragment =
      SplashFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splash_fragment_holder, this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }

  override fun onDestroy() {
    super.onDestroy()

    this.migrationExecutor.shutdown()
  }
}
