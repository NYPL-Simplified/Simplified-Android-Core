package org.nypl.simplified.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.LocalDateTime
import org.librarysimplified.services.api.Services
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AxisNowServiceType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.boot.api.BootFailureTesting
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.cardcreator.CardCreatorDebugging
import org.nypl.simplified.crashlytics.api.CrashlyticsServiceType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.slf4j.LoggerFactory

class SettingsDebugViewModel(application: Application) : AndroidViewModel(application) {

  private val logger =
    LoggerFactory.getLogger(SettingsDebugViewModel::class.java)

  private val services =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val accountRegistry =
    services.requireService(AccountProviderRegistryType::class.java)

  private val analytics =
    services.requireService(AnalyticsType::class.java)

  private val booksController =
    services.requireService(BooksControllerType::class.java)

  private val adeptExecutor =
    services.optionalService(AdobeAdeptExecutorType::class.java)

  private val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val feedLoader =
    services.requireService(FeedLoaderType::class.java)

  private val crashlytics =
    services.optionalService(CrashlyticsServiceType::class.java)

  private val subscriptions =
    CompositeDisposable(
      profilesController
        .profileEvents()
        .subscribe(this::onProfileEvent)
    )

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated.Succeeded) {
      val old = event.oldDescription.preferences
      val new = event.newDescription.preferences
      if (old.showTestingLibraries != new.showTestingLibraries) {
        this.accountRegistry.clear()
        this.accountRegistry.refresh(
          includeTestingLibraries = new.showTestingLibraries
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val crashlyticsId: String =
    when {
      this.crashlytics == null -> "Crashlytics is not enabled."
      this.crashlytics.userId == "" -> "(Unassigned)"
      else -> this.crashlytics.userId
    }

  val appVersion: String =
    buildConfig.simplifiedVersion

  val supportEmailAddress: String =
    buildConfig.supportErrorReportEmailAddress

  val axisNowSupported: Boolean =
    services.optionalService(AxisNowServiceType::class.java) != null

  val adeptSupported: Boolean =
    this.adeptExecutor != null

  val adeptActivations: LiveData<List<AdobeDRMExtensions.Activation>> =
    fetchAdeptActivations()

  var cardCreatorFakeLocation: Boolean
    get() =
      CardCreatorDebugging.fakeNewYorkLocation
    set(value) {
      this.logger.debug("card creator fake location: {}", value)
      CardCreatorDebugging.fakeNewYorkLocation = value
    }

  var useExperimentalR2: Boolean
    get() =
      this.profilesController
        .profileCurrent()
        .preferences()
        .useExperimentalR2
    set(value) {
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(useExperimentalR2 = value))
      }
    }

  var showOnlySupportedBooks: Boolean
    get() =
      this.feedLoader.showOnlySupportedBooks
    set(value) {
      this.feedLoader.showOnlySupportedBooks = value
    }

  var hasSeenLibrarySelection: Boolean
    get() =
      this.profilesController
        .profileCurrent()
        .preferences()
        .hasSeenLibrarySelectionScreen
    set(value) {
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(hasSeenLibrarySelectionScreen = value))
      }
    }

  var isBootFailureEnabled: Boolean
    get() =
      BootFailureTesting.isBootFailureEnabled(getApplication())
    set(value) {
      BootFailureTesting.enableBootFailures(
        context = getApplication(),
        enabled = value
      )
    }

  var showTestingLibraries: Boolean
    get() =
      this.profilesController
        .profileCurrent()
        .preferences()
        .showTestingLibraries
    set(value) {
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(showTestingLibraries = value))
      }
    }

  fun sendAnalytics() {
    this.analytics.publishEvent(
      AnalyticsEvent.SyncRequested(
        timestamp = LocalDateTime.now(),
        credentials = null
      )
    )
  }

  fun sendErrorLogs() {
    Reports.sendReportsDefault(
      context = this.getApplication(),
      address = this.supportEmailAddress,
      subject = "[simplye-error-report] ${this.appVersion}",
      body = ""
    )
  }

  fun syncAccounts() {
    try {
      this.profilesController.profileCurrent()
        .accounts()
        .keys
        .forEach { account ->
          this.booksController.booksSync(account)
        }
    } catch (e: Exception) {
      this.logger.error("ouch: ", e)
    }
  }

  fun forgetAllAnnouncements() {
    try {
      val profile = this.profilesController.profileCurrent()
      val accounts = profile.accounts()
      for ((_, account) in accounts) {
        account.setPreferences(account.preferences.copy(announcementsAcknowledged = listOf()))
      }
    } catch (e: Exception) {
      this.logger.error("could not forget announcements: ", e)
    }
  }

  private fun fetchAdeptActivations(): LiveData<List<AdobeDRMExtensions.Activation>> {
    val activationsLive = MutableLiveData<List<AdobeDRMExtensions.Activation>>(emptyList())

    if (this.adeptExecutor == null) {
      return activationsLive
    }

    val adeptFuture =
      AdobeDRMExtensions.getDeviceActivations(
        this.adeptExecutor,
        { message -> this.logger.error("DRM: {}", message) },
        { message -> this.logger.debug("DRM: {}", message) }
      )

    adeptFuture.addListener(
      {
        val activations = try {
          adeptFuture.get()
        } catch (e: Exception) {
          this.logger.error("could not retrieve activations: ", e)
          emptyList()
        }
        activationsLive.postValue(activations)
      },
      MoreExecutors.directExecutor()
    )

    return activationsLive
  }
}
