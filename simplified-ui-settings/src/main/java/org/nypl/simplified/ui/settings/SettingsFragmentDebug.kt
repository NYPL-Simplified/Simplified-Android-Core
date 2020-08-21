package org.nypl.simplified.ui.settings

import android.app.AlertDialog
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.joda.time.LocalDateTime
import org.librarysimplified.services.api.Services
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.boot.api.BootFailureTesting
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.cardcreator.CardCreatorDebugging
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfileUpdated.Succeeded
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows various debug options for testing app functionality at runtime.
 */

class SettingsFragmentDebug : Fragment() {

  private val logger =
    LoggerFactory.getLogger(SettingsFragmentDebug::class.java)

  private val appVersion by lazy {
    try {
      val context = this.requireContext()
      val pkgManager = context.packageManager
      val pkgInfo = pkgManager.getPackageInfo(context.packageName, 0)
      "${pkgInfo.versionName} (${pkgInfo.versionCode})"
    } catch (e: NameNotFoundException) {
      "Unavailable"
    }
  }

  private val navigationController by lazy {
    NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }

  private lateinit var accountRegistry: AccountProviderRegistryType
  private lateinit var adobeDRMActivationTable: TableLayout
  private lateinit var analytics: AnalyticsType
  private lateinit var booksController: BooksControllerType
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var cacheButton: Button
  private lateinit var cardCreatorFakeLocation: SwitchCompat
  private lateinit var crashButton: Button
  private lateinit var customOPDS: Button
  private lateinit var drmTable: TableLayout
  private lateinit var enableR2: SwitchCompat
  private lateinit var failNextBoot: SwitchCompat
  private lateinit var hasSeenLibrarySelection: SwitchCompat
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var sendAnalyticsButton: Button
  private lateinit var sendReportButton: Button
  private lateinit var showErrorButton: Button
  private lateinit var showTesting: SwitchCompat
  private lateinit var syncAccountsButton: Button
  private lateinit var toolbar: Toolbar
  private lateinit var uiThread: UIThreadServiceType

  private var adeptExecutor: AdobeAdeptExecutorType? = null
  private var profileEventSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.booksController =
      services.requireService(BooksControllerType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.accountRegistry =
      services.requireService(AccountProviderRegistryType::class.java)
    this.analytics =
      services.requireService(AnalyticsType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.adeptExecutor =
      services.optionalService(AdobeAdeptExecutorType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.settings_debug, container, false)

    this.crashButton =
      view.findViewById(R.id.settingsVersionDevCrash)
    this.cacheButton =
      view.findViewById(R.id.settingsVersionDevShowCacheDir)
    this.sendReportButton =
      view.findViewById(R.id.settingsVersionDevSendReports)
    this.showErrorButton =
      view.findViewById(R.id.settingsVersionDevShowError)
    this.sendAnalyticsButton =
      view.findViewById(R.id.settingsVersionDevSyncAnalytics)
    this.syncAccountsButton =
      view.findViewById(R.id.settingsVersionDevSyncAccounts)
    this.drmTable =
      view.findViewById(R.id.settingsVersionDrmSupport)
    this.adobeDRMActivationTable =
      view.findViewById(R.id.settingsVersionDrmAdobeActivations)
    this.showTesting =
      view.findViewById(R.id.settingsVersionDevProductionLibrariesSwitch)
    this.failNextBoot =
      view.findViewById(R.id.settingsVersionDevFailNextBootSwitch)
    this.hasSeenLibrarySelection =
      view.findViewById(R.id.settingsVersionDevSeenLibrarySelectionScreen)
    this.cardCreatorFakeLocation =
      view.findViewById(R.id.settingsVersionDevCardCreatorLocationSwitch)
    this.enableR2 =
      view.findViewById(R.id.settingsVersionDevEnableR2Switch)
    this.customOPDS =
      view.findViewById(R.id.settingsVersionDevCustomOPDS)

    return view
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar()

    this.crashButton.setOnClickListener {
      throw OutOfMemoryError("Pretending to have run out of memory!")
    }

    this.cacheButton.setOnClickListener {
      val context = this.requireContext()
      val message = StringBuilder(128)
      message.append("Cache directory is: ")
      message.append(context.cacheDir)
      message.append("\n")
      message.append("\n")
      message.append("Exists: ")
      message.append(context.cacheDir?.isDirectory ?: false)
      message.append("\n")

      AlertDialog.Builder(context)
        .setTitle("Cache Directory")
        .setMessage(message.toString())
        .show()
    }

    this.sendReportButton.setOnClickListener {
      Reports.sendReportsDefault(
        context = this.requireContext(),
        address = this.buildConfig.supportErrorReportEmailAddress,
        subject = "[simplye-error-report] ${this.appVersion}",
        body = ""
      )
    }

    /*
     * A button that publishes a "sync requested" event to the analytics system. Registered
     * analytics implementations are expected to respond to this event and publish any buffered
     * data that they may have to remote servers.
     */

    this.sendAnalyticsButton.setOnClickListener {
      this.analytics.publishEvent(
        AnalyticsEvent.SyncRequested(
          timestamp = LocalDateTime.now(),
          credentials = null
        )
      )
      Toast.makeText(
        this.requireContext(),
        "Triggered analytics send",
        Toast.LENGTH_SHORT
      ).show()
    }

    this.showErrorButton.setOnClickListener {
      this.showErrorPage()
    }

    this.syncAccountsButton.setOnClickListener {
      Toast.makeText(
        this.requireContext(),
        "Triggered sync of all accounts",
        Toast.LENGTH_SHORT
      ).show()

      try {
        this.profilesController.profileCurrent()
          .accounts()
          .values
          .forEach { account ->
            this.booksController.booksSync(account)
          }
      } catch (e: Exception) {
        this.logger.error("ouch: ", e)
      }
    }

    this.drmTable.removeAllViews()
    this.drmTable.addView(this.drmACSSupportRow())
    this.adobeDRMActivationTable.removeAllViews()

    this.profileEventSubscription =
      this.profilesController
        .profileEvents()
        .subscribe(this::onProfileEvent)

    this.showTesting.isChecked =
      this.profilesController
        .profileCurrent()
        .preferences()
        .showTestingLibraries

    this.enableR2.isChecked =
      this.profilesController
        .profileCurrent()
        .preferences()
        .useExperimentalR2

    /*
     * Configure the "fail next boot" switch to enable/disable boot failures.
     */

    this.failNextBoot.isChecked = isBootFailureEnabled()
    this.failNextBoot.setOnCheckedChangeListener { _, checked ->
      enableBootFailures(checked)
    }

    /*
     * Configure the "has seen library selection" switch
     */

    this.hasSeenLibrarySelection.isChecked =
      this.profilesController
        .profileCurrent()
        .preferences()
        .hasSeenLibrarySelectionScreen
    this.hasSeenLibrarySelection.setOnCheckedChangeListener { _, isChecked ->
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(hasSeenLibrarySelectionScreen = isChecked))
      }
    }

    /*
     * Configure the custom OPDS button.
     */

    this.customOPDS.setOnClickListener {
      this.navigationController.openSettingsCustomOPDS()
    }

    /*
     * Update the current profile's preferences whenever the testing switch is changed.
     */

    this.showTesting.setOnClickListener {
      val show = this.showTesting.isChecked
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(showTestingLibraries = show))
      }
    }

    this.cardCreatorFakeLocation.isChecked = CardCreatorDebugging.fakeNewYorkLocation
    this.cardCreatorFakeLocation.setOnCheckedChangeListener { _, checked ->
      this.logger.debug("card creator fake location: {}", checked)
      CardCreatorDebugging.fakeNewYorkLocation = checked
    }

    /*
     * Update the current profile's preferences whenever the R2 switch is changed.
     */

    this.enableR2.setOnClickListener {
      val r2 = this.enableR2.isChecked
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(useExperimentalR2 = r2))
      }
    }
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.settingsDebug),
        subtitle = ""
      )
      host.toolbarSetBackArrowConditionally(
        context = host,
        shouldArrowBePresent = {
          this.navigationController.backStackSize() > 1
        },
        onArrowClicked = {
          this.navigationController.popBackStack()
        })
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }

  data class ExampleError(
    override val message: String
  ) : PresentableErrorType

  private fun showErrorPage() {
    val attributes = sortedMapOf(
      Pair("Version", this.appVersion)
    )

    val taskSteps =
      mutableListOf<TaskStep<ExampleError>>()

    taskSteps.add(
      TaskStep(
        "Opening error page.",
        TaskStepResolution.TaskStepSucceeded("Error page successfully opened.")
      )
    )

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report] ${this.appVersion}",
        attributes = attributes,
        taskSteps = taskSteps
      )

    this.navigationController.openErrorPage(parameters)
  }

  private fun enableBootFailures(enabled: Boolean) {
    BootFailureTesting.enableBootFailures(
      context = this.requireContext(),
      enabled = enabled
    )
  }

  private fun isBootFailureEnabled(): Boolean {
    return BootFailureTesting.isBootFailureEnabled(this.requireContext())
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated) {
      this.uiThread.runOnUIThread(Runnable {
        this.showTesting.isChecked = this.profilesController
          .profileCurrent()
          .preferences()
          .showTestingLibraries
      })

      if (event is Succeeded) {
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
  }

  private fun drmACSSupportRow(): TableRow {
    val row =
      this.layoutInflater.inflate(
        R.layout.settings_version_table_item, this.drmTable, false
      ) as TableRow
    val key =
      row.findViewById<TextView>(R.id.key)
    val value =
      row.findViewById<TextView>(R.id.value)

    key.text = "Adobe ACS"

    val executor = this.adeptExecutor
    if (executor == null) {
      value.setTextColor(Color.RED)
      value.text = "Unsupported"
      return row
    }

    /*
     * If we managed to get an executor, then fetch the current activations.
     */

    val adeptFuture =
      AdobeDRMExtensions.getDeviceActivations(
        executor,
        { message -> this.logger.error("DRM: {}", message) },
        { message -> this.logger.debug("DRM: {}", message) })

    adeptFuture.addListener(
      Runnable {
        this.uiThread.runOnUIThread(Runnable {
          try {
            this.onAdobeDRMReceivedActivations(adeptFuture.get())
          } catch (e: Exception) {
            this.onAdobeDRMReceivedActivationsError(e)
          }
        })
      },
      MoreExecutors.directExecutor()
    )

    value.setTextColor(Color.GREEN)
    value.text = "Supported"
    return row
  }

  private fun onAdobeDRMReceivedActivationsError(e: Exception) {
    this.logger.error("could not retrieve activations: ", e)
  }

  private fun onAdobeDRMReceivedActivations(activations: List<AdobeDRMExtensions.Activation>) {
    this.adobeDRMActivationTable.removeAllViews()

    this.run {
      val row =
        this.layoutInflater.inflate(
          R.layout.settings_drm_activation_table_item,
          this.adobeDRMActivationTable,
          false
        ) as TableRow
      val index = row.findViewById<TextView>(R.id.index)
      val vendor = row.findViewById<TextView>(R.id.vendor)
      val device = row.findViewById<TextView>(R.id.device)
      val userName = row.findViewById<TextView>(R.id.userName)
      val userId = row.findViewById<TextView>(R.id.userId)
      val expiry = row.findViewById<TextView>(R.id.expiry)

      index.text = "Index"
      vendor.text = "Vendor"
      device.text = "Device"
      userName.text = "UserName"
      userId.text = "UserID"
      expiry.text = "Expiry"

      this.adobeDRMActivationTable.addView(row)
    }

    for (activation in activations) {
      val row =
        this.layoutInflater.inflate(
          R.layout.settings_drm_activation_table_item,
          this.adobeDRMActivationTable,
          false
        ) as TableRow
      val index = row.findViewById<TextView>(R.id.index)
      val vendor = row.findViewById<TextView>(R.id.vendor)
      val device = row.findViewById<TextView>(R.id.device)
      val userName = row.findViewById<TextView>(R.id.userName)
      val userId = row.findViewById<TextView>(R.id.userId)
      val expiry = row.findViewById<TextView>(R.id.expiry)

      index.text = activation.index.toString()
      vendor.text = activation.vendor.value
      device.text = activation.device.value
      userName.text = activation.userName
      userId.text = activation.userID.value
      expiry.text = activation.expiry ?: "No expiry"

      this.adobeDRMActivationTable.addView(row)
    }
  }

  override fun onStop() {
    super.onStop()

    this.profileEventSubscription?.dispose()
    this.cacheButton.setOnClickListener(null)
    this.crashButton.setOnClickListener(null)
    this.customOPDS.setOnClickListener(null)
    this.failNextBoot.setOnCheckedChangeListener(null)
    this.sendReportButton.setOnClickListener(null)
    this.showErrorButton.setOnClickListener(null)
    this.showTesting.setOnClickListener(null)
  }
}
