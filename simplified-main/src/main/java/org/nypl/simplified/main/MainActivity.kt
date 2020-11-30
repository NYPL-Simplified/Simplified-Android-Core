package org.nypl.simplified.main

import android.app.ActionBar
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.lifecycle.ViewModelProviders
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.Observable
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.documents.EULAType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.migration.api.Migrations
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED
import org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryComplete
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.splash.SplashFragment
import org.nypl.simplified.ui.splash.SplashListenerType
import org.nypl.simplified.ui.splash.SplashParameters
import org.nypl.simplified.ui.splash.SplashSelectionFragment
import org.nypl.simplified.ui.theme.ThemeControl
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit

class MainActivity :
  AppCompatActivity(),
  OnBackStackChangedListener,
  SplashListenerType,
  ErrorPageListenerType {

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private val migrationExecutor: ListeningScheduledExecutorService =
    NamedThreadPools.namedThreadPool(1, "migrations", 19)

  private lateinit var mainViewModel: MainFragmentViewModel
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesNavigationController: ProfilesNavigationController

  private val navigationController: NavigationControllerType?
    get() {
      val controllers = arrayListOf(
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          CatalogNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          AccountNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          SettingsNavigationControllerType::class.java
        ),
        this.navigationControllerDirectory.navigationControllerIfAvailable(
          ProfilesNavigationControllerType::class.java
        )
      )
      return controllers.filterNotNull().firstOrNull()
    }

  private fun getSplashService(): BrandingSplashServiceType {
    return ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
      )
  }

  private fun getSplashParams(): SplashParameters {
    val migrationReportEmail =
      this.resources.getString(R.string.featureErrorEmail)
        .trim()
        .ifEmpty { null }

    val splashService = getSplashService()
    return SplashParameters(
      textColor = ContextCompat.getColor(this, ThemeControl.themeFallback.color),
      background = Color.WHITE,
      splashMigrationReportEmail = migrationReportEmail,
      splashImageResource = splashService.splashImageResource(),
      splashImageTitleResource = splashService.splashImageTitleResource(),
      splashImageSeconds = 2L,
      showLibrarySelection = splashService.shouldShowLibrarySelectionScreen
    )
  }

  private fun getAvailableEULA(): EULAType? {
    return Services.serviceDirectory()
      .requireService(DocumentStoreType::class.java)
      .eula
  }

  private fun doLoginAccount(
    profilesController: ProfilesControllerType,
    account: AccountType,
    credentials: AccountAuthenticationCredentials
  ): TaskResult<Unit> {
    this.logger.debug("doLoginAccount")

    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Logging in...")

    if (account.provider.authenticationAlternatives.isEmpty()) {
      when (val description = account.provider.authentication) {
        is AccountProviderAuthenticationDescription.COPPAAgeGate,
        AccountProviderAuthenticationDescription.Anonymous -> {
          return taskRecorder.finishSuccess(Unit)
        }
        is AccountProviderAuthenticationDescription.Basic -> {
          when (credentials) {
            is AccountAuthenticationCredentials.Basic -> {
              return profilesController.profileAccountLogin(
                ProfileAccountLoginRequest.Basic(
                  account.id,
                  description,
                  credentials.userName,
                  credentials.password
                )
              ).get(3L, TimeUnit.MINUTES)
            }
            is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
              val message = "Can't use OAuth authentication during migrations."
              taskRecorder.currentStepFailed(message, "missingInformation")
              return taskRecorder.finishFailure()
            }
            is AccountAuthenticationCredentials.SAML2_0 -> {
              val message = "Can't use SAML 2.0 authentication during migrations."
              taskRecorder.currentStepFailed(message, "missingInformation")
              return taskRecorder.finishFailure()
            }
          }
        }
        is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
          val message = "Can't use OAuth authentication during migrations."
          taskRecorder.currentStepFailed(message, "missingInformation")
          return taskRecorder.finishFailure()
        }
        is AccountProviderAuthenticationDescription.SAML2_0 -> {
          val message = "Can't use SAML 2.0 authentication during migrations."
          taskRecorder.currentStepFailed(message, "missingInformation")
          return taskRecorder.finishFailure()
        }
      }
    } else {
      val message = "Can't determine which authentication method is required."
      taskRecorder.currentStepFailed(message, "missingInformation")
      return taskRecorder.finishFailure()
    }
  }

  private fun doCreateAccount(
    profilesController: ProfilesControllerType,
    provider: URI
  ): TaskResult<AccountType> {
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

    val splashFragment = SplashFragment.newInstance(getSplashParams())
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, splashFragment, "SPLASH_MAIN")
      .commit()
  }

  private fun onStartupFinished() {
    this.logger.debug("onStartupFinished")

    val services =
      Services.serviceDirectory()
    val profilesController =
      services.requireService(ProfilesControllerType::class.java)
    val accountProviders =
      services.requireService(AccountProviderRegistryType::class.java)
    val splashService = getSplashService()

    this.navigationControllerDirectory.removeNavigationController(
      AccountNavigationControllerType::class.java
    )

    return when (profilesController.profileAnonymousEnabled()) {
      ANONYMOUS_PROFILE_ENABLED -> {
        val profile = profilesController.profileCurrent()
        val defaultProvider = accountProviders.defaultProvider

        val hasNonDefaultAccount =
          profile.accounts().values.count { it.provider.id != defaultProvider.id } > 0
        this.logger.debug("hasNonDefaultAccount=$hasNonDefaultAccount")

        val shouldShowLibrarySelectionScreen =
          splashService.shouldShowLibrarySelectionScreen && !profile.preferences().hasSeenLibrarySelectionScreen
        this.logger.debug("shouldShowLibrarySelectionScreen=$shouldShowLibrarySelectionScreen")

        if (!hasNonDefaultAccount && shouldShowLibrarySelectionScreen) {
          this.openLibrarySelectionScreen()
        } else {
          this.openCatalog()
        }
      }
      ANONYMOUS_PROFILE_DISABLED -> {
        this.openProfileScreen()
      }
    }
  }

  private fun openLibrarySelectionScreen() {
    val fragment =
      SplashSelectionFragment.newInstance(getSplashParams())
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "SPLASH_MAIN")
      .commit()
    this.supportActionBar?.hide()
  }

  private fun openProfileScreen() {
    this.mainViewModel.clearHistory = true

    val profilesFragment = ProfileSelectionFragment()
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, profilesFragment, "MAIN")
      .commit()
    this.supportActionBar?.hide()
  }

  private fun openCatalog() {
    // Sanity check; we were seeing some crashes on startup when performing a
    // transaction after the fragment manager had been destroyed.
    if (this.isFinishing || this.supportFragmentManager.isDestroyed) return

    ViewModelProviders.of(this)
      .get(MainFragmentViewModel::class.java)
      .clearHistory = true

    val mainFragment = MainFragment()
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
      .commit()
    this.supportActionBar?.show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate (recreating {})", savedInstanceState != null)
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

    this.navigationControllerDirectory = NavigationControllers.findDirectory(this)
    this.setContentView(R.layout.main_host)

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)
    this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    this.supportActionBar?.setDisplayShowHomeEnabled(true)
    this.supportActionBar?.hide() // Hide toolbar until requested

    this.supportFragmentManager
      .addOnBackStackChangedListener(this)

    this.mainViewModel =
      ViewModelProviders.of(this)
        .get(MainFragmentViewModel::class.java)

    this.profilesNavigationController =
      ProfilesNavigationController(this.supportFragmentManager, this.mainViewModel)
    this.navigationControllerDirectory.updateNavigationController(
      ProfilesNavigationControllerType::class.java, this.profilesNavigationController
    )

    if (savedInstanceState == null) {
      this.mainViewModel.clearHistory = true
      this.showSplashScreen()
    }
  }

  override fun getActionBar(): ActionBar? {
    throw UnsupportedOperationException("Use 'getSupportActionBar' instead")
  }

  override fun onBackPressed() {
    this.navigationController?.let { controller ->
      this.logger.debug("delivering back press to {}", controller::class.simpleName)
      if (!controller.popBackStack()) {
        super.onBackPressed()
      }
      return
    }

    this.logger.debug("delivering back press to activity")
    super.onBackPressed()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        this.navigationController?.let { controller ->
          this.logger.debug("delivering home press to {}", controller::class.simpleName)
          controller.popToRoot()
        } ?: false
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackStackChanged() {
    this.navigationController?.let { controller ->
      val isRoot = (1 == controller.backStackSize())
      this.logger.debug(
        "controller stack size changed [{}, isRoot={}]", controller.backStackSize(), isRoot
      )
      this.supportActionBar?.apply {
        setHomeAsUpIndicator(null)
        setHomeActionContentDescription(null)
        setDisplayHomeAsUpEnabled(!isRoot)
      }
    }
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
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    profilesController.profileSelect(profilesController.profileCurrent().id)
  }

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    this.logger.debug("onSplashWantProfilesMode")

    return Services.serviceDirectory()
      .requireService(ProfilesControllerType::class.java)
      .profileAnonymousEnabled()
  }

  override fun onSplashWantMigrations(): MigrationsType {
    val profilesController =
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    val isAnonymous =
      profilesController.profileAnonymousEnabled() == ANONYMOUS_PROFILE_ENABLED

    val migrationServiceDependencies =
      MigrationServiceDependencies(
        createAccount = { uri ->
          this.doCreateAccount(profilesController, uri)
        },
        loginAccount = { account, credentials ->
          this.doLoginAccount(profilesController, account, credentials)
        },
        accountEvents = profilesController.accountEvents(),
        applicationProfileIsAnonymous = isAnonymous,
        applicationVersion = this.applicationVersion(),
        context = this
      )

    return Migrations.create(migrationServiceDependencies)
  }

  override fun onSplashWantMigrationExecutor(): ListeningExecutorService {
    return this.migrationExecutor
  }

  override fun onSplashMigrationReport(report: MigrationReport) {
    // No longer used
  }

  override fun onSplashLibrarySelectionWanted() {
    val fm = this.supportFragmentManager

    /*
     * Set up a custom navigation controller used by the settings library registry screen. It's
     * only capable of moving to the error page, or popping the back stack.
     */

    this.navigationControllerDirectory.updateNavigationController(
      AccountNavigationControllerType::class.java,
      object : AccountNavigationControllerType {
        override fun popBackStack(): Boolean {
          this@MainActivity.onStartupFinished()
          return true
        }

        override fun popToRoot(): Boolean {
          this@MainActivity.onStartupFinished()
          return true
        }

        override fun backStackSize(): Int {
          // Note: Little hack to get the Toolbar to display correctly.
          return fm.backStackEntryCount + 1
        }

        override fun openSettingsAccount(parameters: AccountFragmentParameters) {
          throw UnreachableCodeException()
        }

        override fun openErrorPage(parameters: ErrorPageParameters) {
          throw UnreachableCodeException()
        }

        override fun openSAML20Login(parameters: AccountSAML20FragmentParameters) {
          throw UnreachableCodeException()
        }

        override fun openSettingsAccountRegistry() {
          throw UnreachableCodeException()
        }
      }
    )

    val fragment = AccountListRegistryFragment()
    fm.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "MAIN")
      .addToBackStack(null)
      .commit()
    this.supportActionBar?.show()
  }

  override fun onSplashLibrarySelectionNotWanted() {
    val profilesController =
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    /*
     * Store the fact that we've seen the selection screen.
     */

    profilesController.profileUpdate { profileDescription ->
      profileDescription.copy(
        preferences = profileDescription.preferences.copy(hasSeenLibrarySelectionScreen = true)
      )
    }
    this.openCatalog()
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters) {
    Reports.sendReportsDefault(
      context = this,
      address = parameters.emailAddress,
      subject = parameters.subject,
      body = parameters.body
    )
  }

  override fun onNewIntent(intent: Intent) {
    if (Services.isInitialized()) {
      if (this.tryToCompleteOAuthIntent(intent)) {
        return
      }
    }
    super.onNewIntent(intent)
  }

  private fun tryToCompleteOAuthIntent(
    intent: Intent
  ): Boolean {
    this.logger.debug("attempting to parse incoming intent as OAuth token")

    val buildConfiguration =
      Services.serviceDirectory()
        .requireService(BuildConfigurationServiceType::class.java)

    val result = OAuthCallbackIntentParsing.processIntent(
      intent = intent,
      requiredScheme = buildConfiguration.oauthCallbackScheme.scheme,
      parseUri = Uri::parse
    )

    if (result is OAuthParseResult.Failed) {
      this.logger.warn("failed to parse incoming intent: {}", result.message)
      return false
    }

    this.logger.debug("parsed OAuth token")
    val accountId = AccountID((result as OAuthParseResult.Success).accountId)
    val token = result.token

    val profilesController =
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)

    profilesController.profileAccountLogin(
      OAuthWithIntermediaryComplete(
        accountId = accountId,
        token = token
      )
    )
    return true
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
