package org.nypl.simplified.tests.sandbox

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.migration.api.Migrations
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.ui.splash.SplashFragment
import org.nypl.simplified.ui.splash.SplashListenerType
import org.nypl.simplified.ui.splash.SplashParameters
import org.nypl.simplified.ui.theme.ThemeControl
import java.net.URL
import java.util.concurrent.Executors

class SplashActivity : AppCompatActivity(), SplashListenerType {

  override fun onSplashDone() {
    this.finish()
  }

  override fun onSplashWantMigrations(): MigrationsType {
    return migrations
  }

  override fun onSplashWantMigrationExecutor(): ListeningExecutorService {
    return this.migrationExecutor
  }

  override fun onSplashMigrationReport(report: MigrationReport) {
  }

  override fun onSplashLibrarySelectionWanted() {
  }

  override fun onSplashLibrarySelectionNotWanted() {
  }

  private val migrationExecutor =
    MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

  private val executor =
    MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

  private val bootEvents =
    PublishSubject.create<BootEvent>()

  private val bootFuture =
    this.executor.submit {
      for (i in 0 until 5) {
        this.bootEvents.onNext(BootEvent.BootInProgress("Loading $i"))
        Thread.sleep(1000)
      }
      this.bootEvents.onNext(BootEvent.BootCompleted("Loaded!"))
    }

  override fun onSplashWantBootFuture(): ListenableFuture<*> {
    return this.bootFuture
  }

  override fun onSplashWantBootEvents(): Observable<BootEvent> =
    this.bootEvents

  override fun onSplashEULAIsProvided(): Boolean {
    return true
  }

  override fun onSplashEULARequested(): EULAType {
    return object : EULAType {
      override fun eulaHasAgreed(): Boolean {
        return false
      }

      override fun documentSetLatestURL(u: URL?) {
      }

      override fun documentGetReadableURL(): URL {
        return URL("http://www.librarysimplified.org/EULA.html")
      }

      override fun eulaSetHasAgreed(t: Boolean) {
      }
    }
  }

  override fun onSplashOpenProfileAnonymous() {
  }

  override fun onSplashWantProfilesMode(): ProfilesDatabaseType.AnonymousProfileEnabled {
    return ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
  }

  private lateinit var splashMainFragment: SplashFragment
  private lateinit var parameters: SplashParameters

  private val migrations =
    Migrations.create(
      MigrationServiceDependencies(
        accountEvents = PublishSubject.create(),
        createAccount = {
          val taskRecorder =
            TaskRecorder.create<AccountCreateErrorDetails>()
          taskRecorder.beginNewStep("OK")
          taskRecorder.finishFailure()
        },
        loginAccount = { _, _ ->
          val taskRecorder =
            TaskRecorder.create<AccountLoginErrorData>()
          taskRecorder.beginNewStep("OK")
          taskRecorder.finishFailure()
        },
        applicationProfileIsAnonymous = true,
        applicationVersion = "Sandbox 0.0.1",
        context = this
      ))

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setTheme(R.style.SimplifiedTheme_NoActionBar_DeepPurple)
    this.setContentView(R.layout.splash_host)

    this.parameters =
      SplashParameters(
        textColor = resources.getColor(ThemeControl.themeFallback.color),
        background = Color.WHITE,
        splashMigrationReportEmail = "co+org.librarysimplified.tests.sandbox@io7m.com",
        splashImageResource = R.drawable.sandbox,
        splashImageTitleResource = R.drawable.sandbox,
        splashImageSeconds = 2L)

    this.splashMainFragment =
      SplashFragment.newInstance(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.splashHolder, this.splashMainFragment, "SPLASH_MAIN")
      .commit()
  }
}
