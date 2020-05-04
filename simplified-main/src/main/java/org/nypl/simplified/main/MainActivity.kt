package org.nypl.simplified.main

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Some
import io.reactivex.Observable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.migration.api.Migrations
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentParameters
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentServiceType
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.splash.SplashFragment
import org.nypl.simplified.ui.splash.SplashListenerType
import org.nypl.simplified.ui.splash.SplashParameters
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit

class MainActivity :
  AppCompatActivity(),
  SplashListenerType,
  ToolbarHostType,
  ErrorPageListenerType {

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private lateinit var mainViewModel: MainFragmentViewModel
  private lateinit var migrationExecutor: ListeningScheduledExecutorService
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesNavigationController: ProfilesNavigationController
  private lateinit var splashMainFragment: SplashFragment
  private lateinit var toolbar: Toolbar

  private fun getAvailableEULA(): EULAType? {
    val eulaOpt =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
        .requireService(DocumentStoreType::class.java)
        .eula

    return if (eulaOpt is Some<EULAType>) {
      eulaOpt.get()
    } else {
      null
    }
  }

  private fun doLoginAccount(
    profilesController: ProfilesControllerType,
    account: AccountType,
    credentials: AccountAuthenticationCredentials
  ): TaskResult<AccountLoginState.AccountLoginErrorData, Unit> {
    this.logger.debug("doLoginAccount")
    return profilesController.profileAccountLogin(account.id, credentials)
      .get(3L, TimeUnit.MINUTES)
  }

  private fun doCreateAccount(
    profilesController: ProfilesControllerType,
    provider: URI
  ): TaskResult<AccountCreateErrorDetails, AccountType> {
    this.logger.debug("doCreateAccount")
    return profilesController.profileAccountCreateOrReturnExisting(provider)
      .get(3L, TimeUnit.MINUTES)
  }

  private fun applicationVersion(): String {
    return try {
      val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
      "${packageInfo.packageName} ${packageInfo.versionName} (${packageInfo.versionCode})"
    } catch (e: Exception) {
      this.logger.error("could not get package info: ", e)
      "unknown"
    }
  }

  private fun showSplashScreen() {
    this.logger.debug("showSplashScreen")

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

    this.logger.debug("using splash service: ${splashService.javaClass.canonicalName}")

    val migrationReportingEmail =
      this.resources.getString(R.string.featureErrorEmail)
        .trim()
        .let { text -> if (text.isEmpty()) null else text }

    val parameters =
      SplashParameters(
        textColor = this.resources.getColor(ThemeControl.themeFallback.color),
        background = Color.WHITE,
        splashMigrationReportEmail = migrationReportingEmail,
        splashImageResource = splashService.splashImageResource(),
        splashImageSeconds = 2L)

    this.splashMainFragment =
      SplashFragment.newInstance(parameters)

    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }

  private fun onStartupFinished() {
    this.logger.debug("onStartupFinished")

    val profilesController =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
        .requireService(ProfilesControllerType::class.java)

    return when (profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
        this.openCatalog()
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.openProfileScreen()
      }
    }
  }

  private class ProfilesNavigationController(
    private val supportFragmentManager: FragmentManager,
    private val mainViewModel: MainFragmentViewModel
  ) : ProfilesNavigationControllerType {

    private val logger =
      LoggerFactory.getLogger(ProfilesNavigationController::class.java)

    private fun openModificationFragment(
      parameters: ProfileModificationFragmentParameters
    ) {
      val fragmentService =
        Services.serviceDirectory()
          .optionalService(ProfileModificationFragmentServiceType::class.java)

      val fragment =
        if (fragmentService != null) {
          this.logger.debug("found a profile modification fragment service: {}", fragmentService)
          fragmentService.createModificationFragment(parameters)
        } else {
          ProfileModificationDefaultFragment.create(parameters)
        }

      this.supportFragmentManager.beginTransaction()
        .replace(R.id.mainFragmentHolder, fragment, "MAIN")
        .addToBackStack(null)
        .commit()
    }

    override fun openMain() {
      this.logger.debug("openMain")
      this.mainViewModel.clearHistory = true

      val mainFragment = MainFragment()
      this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
      this.supportFragmentManager.beginTransaction()
        .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
        .addToBackStack(null)
        .commit()
    }

    override fun openProfileSelect() {
      this.logger.debug("openProfileSelect")
      this.mainViewModel.clearHistory = true

      val newFragment = ProfileSelectionFragment()
      this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
      this.supportFragmentManager.beginTransaction()
        .replace(R.id.mainFragmentHolder, newFragment, "MAIN")
        .commit()
    }

    override fun openProfileModify(id: ProfileID) {
      this.logger.debug("openProfileModify: ${id.uuid}")
      this.openModificationFragment(ProfileModificationFragmentParameters(id))
    }

    override fun openProfileCreate() {
      this.logger.debug("openProfileCreate")
      this.openModificationFragment(ProfileModificationFragmentParameters(null))
    }

    override fun popBackStack(): Boolean {
      this.logger.debug("popBackStack")
      this.supportFragmentManager.popBackStack()
      return this.supportFragmentManager.backStackEntryCount > 0
    }

    override fun backStackSize(): Int {
      this.logger.debug("backStackSize")
      return this.supportFragmentManager.backStackEntryCount
    }
  }

  private fun openProfileScreen() {
    this.mainViewModel.clearHistory = true

    val profilesFragment = ProfileSelectionFragment()
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, profilesFragment, "MAIN")
      .commit()
  }

  private fun openCatalog() {
    ViewModelProviders.of(this)
      .get(MainFragmentViewModel::class.java)
      .clearHistory = true

    val mainFragment = MainFragment()
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
      .commit()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate (recreating {})", savedInstanceState != null)
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

    this.navigationControllerDirectory = NavigationControllers.findDirectory(this)
    this.setContentView(R.layout.main_host)

    this.toolbar = this.findViewById(R.id.mainToolbar)
    this.toolbar.visibility = View.GONE

    this.mainViewModel =
      ViewModelProviders.of(this)
        .get(MainFragmentViewModel::class.java)

    this.profilesNavigationController =
      ProfilesNavigationController(this.supportFragmentManager, this.mainViewModel)
    this.navigationControllerDirectory.updateNavigationController(
      ProfilesNavigationControllerType::class.java, this.profilesNavigationController)

    if (savedInstanceState == null) {
      this.mainViewModel.clearHistory = true
      this.showSplashScreen()
    }
  }

  override fun onBackPressed() {
    val mainController =
      this.navigationControllerDirectory.navigationControllerIfAvailable(
        CatalogNavigationControllerType::class.java)

    if (mainController != null) {
      this.logger.debug("delivering back press to catalog navigation controller")
      if (!mainController.popBackStack()) {
        super.onBackPressed()
      }
      return
    }

    val profilesNavigationController =
      this.navigationControllerDirectory.navigationControllerIfAvailable(
        ProfilesNavigationControllerType::class.java)

    if (profilesNavigationController != null) {
      this.logger.debug("delivering back press to profiles navigation controller")
      if (!profilesNavigationController.popBackStack()) {
        super.onBackPressed()
      }
      return
    }

    this.logger.debug("delivering back press to activity")
    super.onBackPressed()
  }

  override fun findToolbar(): Toolbar {
    return this.toolbar
  }

  override fun onSplashWantBootFuture(): ListenableFuture<*> {
    this.logger.debug("onSplashWantBootFuture")
    return MainApplication.application.servicesBooting
  }

  override fun onSplashWantBootEvents(): Observable<BootEvent> {
    this.logger.debug("onSplashWantBootEvents")
    return MainApplication.application.servicesBootEvents
  }

  override fun onSplashEULAIsProvided(): Boolean {
    this.logger.debug("onSplashEULAIsProvided")
    return this.getAvailableEULA() != null
  }

  override fun onSplashEULARequested(): EULAType {
    this.logger.debug("onSplashEULARequested")
    return this.getAvailableEULA()!!
  }

  override fun onSplashDone() {
    this.logger.debug("onSplashDone")
    return this.onStartupFinished()
  }

  override fun onSplashOpenProfileAnonymous() {
    this.logger.debug("onSplashOpenProfileAnonymous")

    val profilesController =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
        .requireService(ProfilesControllerType::class.java)

    profilesController.profileSelect(profilesController.profileCurrent().id)
  }

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    this.logger.debug("onSplashWantProfilesMode")

    return Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
      .requireService(ProfilesControllerType::class.java)
      .profileAnonymousEnabled()
  }

  override fun onSplashWantMigrations(): MigrationsType {
    val profilesController =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
        .requireService(ProfilesControllerType::class.java)

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

  override fun onSplashWantMigrationExecutor(): ListeningExecutorService {
    return this.migrationExecutor
  }

  override fun onSplashMigrationReport(report: MigrationReport) {
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters<*>) {
    Reports.sendReportsDefault(
      context = this,
      address = parameters.emailAddress,
      subject = parameters.subject,
      body = parameters.body)
  }

  override fun onUserInteraction() {
    super.onUserInteraction()

    /*
     * Each time the user interacts with something onscreen, reset the timer.
     */

    if (Services.isInitialized()) {
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)
        .profileIdleTimer()
        .reset()
    }
  }
}
