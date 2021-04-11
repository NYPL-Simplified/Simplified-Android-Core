package org.nypl.simplified.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import java.util.ServiceLoader

class BootViewModel(application: Application) : AndroidViewModel(application) {

  private val splashDependencies =
    ServiceLoader
      .load(SplashDependenciesType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${SplashDependenciesType::class.java.canonicalName}"
      )

  private val buildConfig: BuildConfigurationServiceType by lazy {
    ServiceLoader
      .load(BuildConfigurationServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BuildConfigurationServiceType::class.java.canonicalName}"
      )
  }

  private val brandingService: BrandingSplashServiceType =
    ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
      )

  private var failureMessage: String? =
    null

  private val subscriptions =
    CompositeDisposable()

  init {
    splashDependencies
      .bootEvents
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onBootEvent)
      .let { subscriptions.add(it) }
  }

  private fun onBootEvent(event: BootEvent) {
    if (event is BootEvent.BootFailed) {
      failureMessage = event.message
    }
    bootEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val bootEvents: UnicastWorkSubject<BootEvent> =
    UnicastWorkSubject.create()

  val imageResource: Int =
    brandingService.splashImageResource()

  val simplifiedVersion: String =
    buildConfig.simplifiedVersion

  fun sendReport() {
    Reports.sendReportsDefault(
      context = getApplication(),
      address = buildConfig.supportErrorReportEmailAddress,
      subject = "[application startup failure]",
      body = checkNotNull(failureMessage)
    )
  }
}
